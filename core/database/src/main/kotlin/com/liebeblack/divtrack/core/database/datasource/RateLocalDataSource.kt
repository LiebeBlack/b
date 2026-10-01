package com.liebeblack.divtrack.core.database.datasource

import com.liebeblack.divtrack.core.database.DivTrackDatabase
import com.liebeblack.divtrack.core.database.entity.CurrentRateEntity
import com.liebeblack.divtrack.core.database.entity.RateHistoryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de persistencia que consume `:data`.
 *
 * Existe como interfaz (y no se inyecta el DAO directamente) por dos razones:
 * 1. `RateRepositoryImpl` se puede testear en JVM pura con un fake en memoria.
 * 2. La capa de datos no depende de Room en su lógica, solo del contrato.
 */
interface RateLocalDataSource {

    fun observeCurrentRates(): Flow<List<CurrentRateEntity>>

    fun observeHistory(fromEpochDay: Long): Flow<List<RateHistoryEntity>>

    suspend fun upsertCurrentRates(rates: List<CurrentRateEntity>)

    suspend fun upsertHistory(points: List<RateHistoryEntity>)

    suspend fun currentRate(source: String): CurrentRateEntity?

    suspend fun previousClose(source: String, epochDay: Long): RateHistoryEntity?

    suspend fun pruneHistory(beforeEpochDay: Long): Int
}

/** Implementación real sobre Room. */
@Singleton
class RoomRateLocalDataSource @Inject constructor(
    database: DivTrackDatabase,
) : RateLocalDataSource {

    private val dao = database.rateDao()

    override fun observeCurrentRates(): Flow<List<CurrentRateEntity>> = dao.observeCurrentRates()

    override fun observeHistory(fromEpochDay: Long): Flow<List<RateHistoryEntity>> =
        dao.observeHistory(fromEpochDay)

    override suspend fun upsertCurrentRates(rates: List<CurrentRateEntity>) {
        if (rates.isEmpty()) return
        dao.upsertCurrentRates(rates)
    }

    override suspend fun upsertHistory(points: List<RateHistoryEntity>) {
        if (points.isEmpty()) return
        dao.upsertHistory(points)
    }

    override suspend fun currentRate(source: String): CurrentRateEntity? = dao.currentRate(source)

    override suspend fun previousClose(source: String, epochDay: Long): RateHistoryEntity? =
        dao.previousClose(source, epochDay)

    override suspend fun pruneHistory(beforeEpochDay: Long): Int = dao.pruneHistory(beforeEpochDay)
}
