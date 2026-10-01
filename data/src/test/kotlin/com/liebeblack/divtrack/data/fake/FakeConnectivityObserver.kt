package com.liebeblack.divtrack.data.fake

import com.liebeblack.divtrack.core.network.monitor.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Conectividad simulada y conmutable: permite probar el fallo rápido sin red real.
 *
 * El estado vive en un único `MutableStateFlow` del que leen tanto [isOnline] como [observe],
 * así que conmutar la conectividad se ve igual por consulta puntual y por flujo.
 *
 * El conmutador se llama `online` y no `isOnline` a propósito: una propiedad `var isOnline`
 * y el método `isOnline()` del contrato generarían la misma firma JVM (`isOnline()Z`) y el
 * compilador rechaza la clase.
 */
class FakeConnectivityObserver(
    online: Boolean = true,
) : ConnectivityObserver {

    private val state = MutableStateFlow(online)

    /** Conmutador de conveniencia para los tests. */
    var online: Boolean
        get() = state.value
        set(value) {
            state.value = value
        }

    override fun isOnline(): Boolean = state.value

    override fun observe(): Flow<Boolean> = state
}
