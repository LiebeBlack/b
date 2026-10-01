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
    val history = MutableStateFlow<List<RateHistoryEntity>>(emptyList())

    var upsertCurrentRatesCalls: Int = 0
        private set
    var upsertHistoryCalls: Int = 0
        private set

    override fun observeCurrentRates(): Flow<List<CurrentRateEntity>> = currentRates

    override fun observeHistory(fromEpochDay: Long): Flow<List<RateHistoryEntity>> = history

    override suspend fun upsertCurrentRates(rates: List<CurrentRateEntity>) {
        if (rates.isEmpty()) return
        upsertCurrentRatesCalls++
        val bySource = currentRates.value.associateBy { it.source }.toMutableMap()
        rates.forEach { rate -> bySource[rate.source] = rate }
        currentRates.value = bySource.values.toList()
    }

    override suspend fun upsertHistory(points: List<RateHistoryEntity>) {
        if (points.isEmpty()) return
        upsertHistoryCalls++
        val byKey = history.value.associateBy { it.source to it.epochDay }.toMutableMap()
        points.forEach { point -> byKey[point.source to point.epochDay] = point }
        history.value = byKey.values.toList()
    }

    override suspend fun currentRate(source: String): CurrentRateEntity? =
        currentRates.value.firstOrNull { it.source == source }

    override suspend fun previousClose(source: String, epochDay: Long): RateHistoryEntity? =
        history.value
            .filter { it.source == source && it.epochDay < epochDay }
            .maxByOrNull { it.epochDay }

    override suspend fun pruneHistory(beforeEpochDay: Long): Int {
        val before = history.value.size
        history.value = history.value.filter { it.epochDay >= beforeEpochDay }
        return before - history.value.size
    }
}
