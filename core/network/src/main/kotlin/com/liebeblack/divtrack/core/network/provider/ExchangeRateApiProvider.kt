package com.liebeblack.divtrack.core.network.provider

import com.liebeblack.divtrack.core.common.time.TimeProvider
import com.liebeblack.divtrack.core.common.utils.SourceKeys
import com.liebeblack.divtrack.core.network.dto.ExchangeRateApiDto
import com.liebeblack.divtrack.core.network.model.RemoteRate
import com.liebeblack.divtrack.core.network.model.RemoteRateSet
import com.liebeblack.divtrack.core.network.service.ExchangeRateApiService
import javax.inject.Inject

/**
 * Proveedor independiente: publica el oficial VES desde un upstream que no comparte
 * ruta con DolarAPI ni con Yadio.
 *
 * Prioridad 20 (tercero): DolarAPI es más fresco (y aporta el paralelo), Yadio aporta el
 * paralelo upstream; este solo aporta el oficial, así que solo se consulta cuando los dos
 * anteriores no lo trajeron — el `ProviderRegistry` corta la pasada apenas está completo.
 *
 * Contribución esperada: `oficial`. `rates` vacío si el proveedor devolvió un envoltorio
 * de error o un par VES inutilizable: el registro lo cuenta como fallo de este proveedor.
 */
class ExchangeRateApiProvider @Inject constructor(
    private val service: ExchangeRateApiService,
    private val timeProvider: TimeProvider,
) : RateProvider {

    override val id: String = PROVIDER_ID
    override val priority: Int = PRIORITY

    override suspend fun fetchLatest(): RemoteRateSet {
        val dto = service.getLatestUsd()

        if (dto.isFailure) {
            // Un envoltorio "error" es una respuesta válida HTTP con contenido de fallo:
            // se convierte en excepción para que el registro lo trate como caída del proveedor.
            throw IllegalStateException("ExchangeRate-API respondió result=${dto.result ?: "desconocido"}")
        }

        val vesValue = dto.rates[ExchangeRateApiDto.VES_KEY]

        val rates = buildList {
            if (vesValue != null && vesValue > 0.0) {
                add(
                    RemoteRate(
                        sourceKey = SourceKeys.OFICIAL,
                        value = vesValue,
                        updatedAtMillis = dto.timeLastUpdateUnix?.times(ExchangeRateApiDto.SECONDS_TO_MILLIS),
                    ),
                )
            }
        }

        return RemoteRateSet(
            providerId = id,
            rates = rates,
            fetchedAtMillis = timeProvider.nowMillis(),
        )
    }

    companion object {
        const val PROVIDER_ID: String = "ExchangeRateAPI"
        const val PRIORITY: Int = 20
    }
}
