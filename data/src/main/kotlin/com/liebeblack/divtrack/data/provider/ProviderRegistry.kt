package com.liebeblack.divtrack.data.provider

import com.liebeblack.divtrack.core.common.logging.Logger
import com.liebeblack.divtrack.core.common.utils.SourceKeys
import com.liebeblack.divtrack.core.network.error.NetworkErrorMapper
import com.liebeblack.divtrack.core.network.model.RemoteHistoryPoint
import com.liebeblack.divtrack.core.network.model.RemoteRate
import com.liebeblack.divtrack.core.network.provider.RateProvider
import com.liebeblack.divtrack.domain.model.ProviderFailure
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** Resultado agregado de una pasada de tasas vigentes. */
data class ProviderFetchOutcome(
    val rates: List<RemoteRate>,
    val providerIds: List<String>,
    val failures: List<ProviderFailure>,
    val fetchedAtMillis: Long,
)

/** Resultado agregado de una importación de histórico. */
data class ProviderHistoryOutcome(
    val points: List<RemoteHistoryPoint>,
    val providerId: String?,
    val failures: List<ProviderFailure>,
)

/**
 * Orquesta el multi-proveedor con una regla clave: **la resolución es por tasa, no por
 * proveedor**. Si DolarAPI responde solo el oficial, Yadio todavía puede aportar el
 * paralelo, y al revés. La caída de una API nunca deja la app sin ninguna tasa.
 *
 * Los fallos no se propagan: se acumulan en [ProviderFailure] para que el repositorio
 * decida (por ejemplo, mostrar un aviso suave cuando la sincronización fue parcial).
 */
@Singleton
class ProviderRegistry @Inject constructor(
    providers: Set<@JvmSuppressWildcards RateProvider>,
    private val errorMapper: NetworkErrorMapper,
    private val logger: Logger,
) {

    /** Orden estable por prioridad: el proveedor preferido intenta siempre primero. */
    private val orderedProviders: List<RateProvider> = providers.sortedBy { it.priority }

    suspend fun fetchLatest(): ProviderFetchOutcome {
        val resolved = LinkedHashMap<String, RemoteRate>()
        val providerIds = mutableListOf<String>()
        val failures = mutableListOf<ProviderFailure>()
        val fetchedAtMillis = mutableListOf<Long>()

        for (provider in orderedProviders) {
            // Ya está todo resuelto: no gastamos red ni batería en el resto de proveedores.
            if (resolved.keys.containsAll(SourceKeys.all)) break

            fetchSafely { provider.fetchLatest() }
                .onSuccess { remoteSet ->
                    fetchedAtMillis += remoteSet.fetchedAtMillis
                    var contributed = false
                    remoteSet.rates.forEach { rate ->
                        val isKnownSource = rate.sourceKey in SourceKeys.all
                        val isStillMissing = resolved[rate.sourceKey] == null
                        if (isKnownSource && isStillMissing) {
                            resolved[rate.sourceKey] = rate
                            contributed = true
                        }
                    }
                    if (contributed) providerIds += provider.id
                }
                .onFailure { throwable ->
                    failures += ProviderFailure(providerId = provider.id, error = errorMapper.map(throwable))
                    logger.warn(TAG, "Proveedor ${provider.id} falló: ${throwable.message}", throwable)
                }
        }

        return ProviderFetchOutcome(
            rates = resolved.values.toList(),
            providerIds = providerIds.toList(),
            failures = failures.toList(),
            fetchedAtMillis = fetchedAtMillis.maxOrNull() ?: 0L,
        )
    }

    suspend fun fetchHistory(sourceKey: String): ProviderHistoryOutcome {
        val failures = mutableListOf<ProviderFailure>()

        for (provider in orderedProviders) {
            if (!provider.supportsHistory) continue

            try {
                val points = provider.fetchHistory(sourceKey)
                if (points.isNotEmpty()) {
                    return ProviderHistoryOutcome(
                        points = points,
                        providerId = provider.id,
                        failures = failures.toList(),
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                failures += ProviderFailure(providerId = provider.id, error = errorMapper.map(throwable))
                logger.warn(TAG, "Histórico falló en ${provider.id}: ${throwable.message}", throwable)
            }
        }

        return ProviderHistoryOutcome(points = emptyList(), providerId = null, failures = failures.toList())
    }

    /**
     * Aísla el fallo de un proveedor sin romper la cancelación estructurada
     * (`CancellationException` siempre se re-lanza).
     */
    private suspend fun <T> fetchSafely(block: suspend () -> T): kotlin.Result<T> = try {
        kotlin.Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        kotlin.Result.failure(throwable)
    }

    private companion object {
        const val TAG = "ProviderRegistry"
    }
}
