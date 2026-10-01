package com.liebeblack.divtrack.data.fake

import com.liebeblack.divtrack.core.database.datasource.RateLocalDataSource
import com.liebeblack.divtrack.core.database.entity.CurrentRateEntity
import com.liebeblack.divtrack.core.database.entity.RateHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Room simulado en memoria.
 *
 * Reproduce lo que de verdad importa para el repositorio: las escrituras son idempotentes
 * por clave (como el `@Upsert` de Room) y la lectura es un `Flow` que re-emite al cambiar.
 */
class FakeRateLocalDataSource : RateLocalDataSource {

    val currentRates = MutableStateFlow<List<CurrentRateEntity>>(emptyList())
    val dailyCloses = MutableStateFlow<List<RateHistoryEntity>>(emptyList())

    var upsertCurrentRatesCalls: Int = 0
        private set
    var upsertDailyClosesCalls: Int = 0
        private set

    override fun observeCurrentRates(): Flow<List<CurrentRateEntity>> = currentRates

    override suspend fun upsertCurrentRates(rates: List<CurrentRateEntity>) {
        if (rates.isEmpty()) return
        upsertCurrentRatesCalls++
        val bySource = currentRates.value.associateBy { it.source }.toMutableMap()
        rates.forEach { rate -> bySource[rate.source] = rate }
        currentRates.value = bySource.values.toList()
    }

    override suspend fun upsertDailyCloses(closes: List<RateHistoryEntity>) {
        if (closes.isEmpty()) return
        upsertDailyClosesCalls++
        val byKey = dailyCloses.value.associateBy { it.source to it.epochDay }.toMutableMap()
        closes.forEach { close -> byKey[close.source to close.epochDay] = close }
        dailyCloses.value = byKey.values.toList()
    }

    override suspend fun currentRate(source: String): CurrentRateEntity? =
        currentRates.value.firstOrNull { it.source == source }

    override suspend fun previousClose(source: String, epochDay: Long): RateHistoryEntity? =
        dailyCloses.value
            .filter { it.source == source && it.epochDay < epochDay }
            .maxByOrNull { it.epochDay }

    override suspend fun pruneDailyCloses(beforeEpochDay: Long): Int {
        val before = dailyCloses.value.size
        dailyCloses.value = dailyCloses.value.filter { it.epochDay >= beforeEpochDay }
        return before - dailyCloses.value.size
    }
}
