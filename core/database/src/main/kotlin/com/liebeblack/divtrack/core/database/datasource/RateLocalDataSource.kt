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

    suspend fun upsertCurrentRates(rates: List<CurrentRateEntity>)

    suspend fun upsertDailyCloses(closes: List<RateHistoryEntity>)

    suspend fun currentRate(source: String): CurrentRateEntity?

    suspend fun previousClose(source: String, epochDay: Long): RateHistoryEntity?

    suspend fun pruneDailyCloses(beforeEpochDay: Long): Int
}

/** Implementación real sobre Room. */
@Singleton
class RoomRateLocalDataSource @Inject constructor(
    database: DivTrackDatabase,
) : RateLocalDataSource {

    private val dao = database.rateDao()

    override fun observeCurrentRates(): Flow<List<CurrentRateEntity>> = dao.observeCurrentRates()

    override suspend fun upsertCurrentRates(rates: List<CurrentRateEntity>) {
        if (rates.isEmpty()) return
        dao.upsertCurrentRates(rates)
    }

    override suspend fun upsertDailyCloses(closes: List<RateHistoryEntity>) {
        if (closes.isEmpty()) return
        dao.upsertDailyCloses(closes)
    }

    override suspend fun currentRate(source: String): CurrentRateEntity? = dao.currentRate(source)

    override suspend fun previousClose(source: String, epochDay: Long): RateHistoryEntity? =
        dao.previousClose(source, epochDay)

    override suspend fun pruneDailyCloses(beforeEpochDay: Long): Int =
        dao.pruneDailyCloses(beforeEpochDay)
}
