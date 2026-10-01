package com.liebeblack.divtrack.core.network.provider

import com.liebeblack.divtrack.core.common.time.TimeProvider
import com.liebeblack.divtrack.core.common.utils.SourceKeys
import com.liebeblack.divtrack.core.network.mapper.IsoParsers
import com.liebeblack.divtrack.core.network.model.RemoteHistoryPoint
import com.liebeblack.divtrack.core.network.model.RemoteRate
import com.liebeblack.divtrack.core.network.model.RemoteRateSet
import com.liebeblack.divtrack.core.network.service.DolarApiService
import javax.inject.Inject

/**
 * Proveedor primario: entrega oficial y paralelo en una sola llamada y además el histórico
 * diario que alimenta el gráfico YTD.
 */
class DolarApiProvider @Inject constructor(
    private val service: DolarApiService,
    private val timeProvider: TimeProvider,
) : RateProvider {

    override val id: String = PROVIDER_ID
    override val priority: Int = PRIORITY
    override val supportsHistory: Boolean = true

    override suspend fun fetchLatest(): RemoteRateSet {
        val rates = service.getDollars().mapNotNull { dto ->
            val value = dto.usableValue ?: return@mapNotNull null
            if (value <= 0.0) return@mapNotNull null

            RemoteRate(
                sourceKey = dto.fuente.lowercase(),
                value = value,
                updatedAtMillis = IsoParsers.instantMillisOrNull(dto.fechaActualizacion),
            )
        }

        return RemoteRateSet(
            providerId = id,
            rates = rates,
            fetchedAtMillis = timeProvider.nowMillis(),
        )
    }

    override suspend fun fetchHistory(sourceKey: String): List<RemoteHistoryPoint> {
        val normalizedSource = sourceKey.lowercase()
        if (normalizedSource !in SourceKeys.all) return emptyList()

        return service.getHistory(normalizedSource).mapNotNull { dto ->
            val value = dto.usableValue ?: return@mapNotNull null
            if (value <= 0.0) return@mapNotNull null
            val date = IsoParsers.localDateOrNull(dto.fecha) ?: return@mapNotNull null

            RemoteHistoryPoint(
                sourceKey = dto.fuente.lowercase(),
                epochDay = date.toEpochDay(),
                value = value,
            )
        }
    }

    companion object {
        const val PROVIDER_ID: String = "DolarAPI"
        const val PRIORITY: Int = 0
    }
}
