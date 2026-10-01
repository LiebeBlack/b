package com.liebeblack.divtrack.core.network.provider

import com.liebeblack.divtrack.core.common.time.TimeProvider
import com.liebeblack.divtrack.core.common.utils.SourceKeys
import com.liebeblack.divtrack.core.network.model.RemoteHistoryPoint
import com.liebeblack.divtrack.core.network.model.RemoteRate
import com.liebeblack.divtrack.core.network.model.RemoteRateSet
import com.liebeblack.divtrack.core.network.service.YadioService
import javax.inject.Inject

/**
 * Proveedor de respaldo: publica únicamente el paralelo (es la fuente upstream de esa tasa),
 * así que si DolarAPI cae, el paralelo sigue llegando y solo se degrada el oficial.
 *
 * No ofrece histórico diario en esta app ([supportsHistory] = false).
 */
class YadioProvider @Inject constructor(
    private val service: YadioService,
    private val timeProvider: TimeProvider,
) : RateProvider {

    override val id: String = PROVIDER_ID
    override val priority: Int = PRIORITY
    override val supportsHistory: Boolean = false

    override suspend fun fetchLatest(): RemoteRateSet {
        val vesValue = service.getRates().ves

        val rates = buildList {
            if (vesValue != null && vesValue > 0.0) {
                add(
                    RemoteRate(
                        sourceKey = SourceKeys.PARALELO,
                        value = vesValue,
                        // Yadio no publica la hora de su cotización.
                        updatedAtMillis = null,
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

    override suspend fun fetchHistory(sourceKey: String): List<RemoteHistoryPoint> = emptyList()

    companion object {
        const val PROVIDER_ID: String = "Yadio"
        const val PRIORITY: Int = 10
    }
}
