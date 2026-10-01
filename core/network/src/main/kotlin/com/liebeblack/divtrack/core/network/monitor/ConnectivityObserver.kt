package com.liebeblack.divtrack.core.network.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.liebeblack.divtrack.core.common.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/** Estado de conectividad observable, sin acoplarse a `ConnectivityManager` en el resto del código. */
interface ConnectivityObserver {

    /** Consulta puntual (barata, para decidir si vale la pena sincronizar). */
    fun isOnline(): Boolean

    /** Flujo de cambios; solo emite cuando el estado cambia. */
    fun observe(): Flow<Boolean>
}

/**
 * Implementación real sobre `ConnectivityManager`.
 *
 * **Regla de oro: este observador nunca puede dejar la app muda.** El repositorio lo usa
 * para fallar rápido, así que un `false` equivocado se convierte en "Sin conexión" sin
 * haber intentado nada. Por eso solo devuelve `false` cuando el sistema dice que **no hay
 * ninguna red activa**, y en cualquier duda (permiso revocado, ROM que no reporta la
 * capacidad `INTERNET` con ciertas VPN o portales cautivos) responde `true` y deja que
 * OkHttp decida: un error de red real tarda menos que un falso "no hay internet".
 */
@Singleton
class AndroidConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger,
) : ConnectivityObserver {

    private val connectivityManager: ConnectivityManager? =
        runCatching { context.getSystemService(ConnectivityManager::class.java) }.getOrNull()

    override fun isOnline(): Boolean {
        // Sin servicio de conectividad no se bloquea la red: se intenta y se falla con
        // el error real, que es lo que el usuario necesita ver.
        val manager = connectivityManager ?: return true

        return runCatching {
            val activeNetwork = manager.activeNetwork ?: return@runCatching false
            val capabilities = manager.getNetworkCapabilities(activeNetwork)
                ?: return@runCatching false
            capabilities.canReachTheInternet()
        }.getOrElse { throwable ->
            // SecurityException si ACCESS_NETWORK_STATE no estuviera concedido.
            logger.warn(TAG, "No se pudo consultar la conectividad: ${throwable.message}", throwable)
            true
        }
    }

    override fun observe(): Flow<Boolean> = callbackFlow {
        val manager = connectivityManager
        if (manager == null) {
            trySend(true)
            awaitClose { }
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(isOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(networkCapabilities.canReachTheInternet())
            }
        }

        trySend(isOnline())
        try {
            manager.registerDefaultNetworkCallback(callback)
        } catch (security: SecurityException) {
            logger.warn(TAG, "Sin permiso para observar la red: ${security.message}", security)
            trySend(true)
            awaitClose { }
            return@callbackFlow
        }
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().conflate()

    private companion object {
        const val TAG = "ConnectivityObserver"
    }
}

/**
 * Una red sirve para la app si tiene capacidad de internet **o** es un transporte por el
 * que claramente se sale a la red.
 *
 * El `||` no es redundante: hay ROMs y VPN donde el sistema no marca
 * `NET_CAPABILITY_INTERNET` aunque wifi/datos funcionen perfectamente, y confiar solo en esa
 * capacidad es exactamente lo que dejaba la app en "sin conexión" con red de sobra.
 */
private fun NetworkCapabilities.canReachTheInternet(): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
        hasTransport(NetworkCapabilities.TRANSPORT_VPN)
