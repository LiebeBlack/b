package com.liebeblack.divtrack.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.liebeblack.divtrack.core.database.entity.CurrentRateEntity
import com.liebeblack.divtrack.core.database.entity.RateHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a datos de tasas. Las lecturas son `Flow`: Room re-emite al escribir, y esa es
 * la pieza que hace que la UI se actualice sola (online y offline) sin polling.
 */
@Dao
interface RateDao {

    @Query("SELECT * FROM ${CurrentRateEntity.TABLE_NAME}")
    fun observeCurrentRates(): Flow<List<CurrentRateEntity>>

    /** Valor vigente puntual: se usa como "cierre anterior" en la primera sincronización. */
    @Query("SELECT * FROM ${CurrentRateEntity.TABLE_NAME} WHERE source = :source LIMIT 1")
    suspend fun currentRate(source: String): CurrentRateEntity?

    @Upsert
    suspend fun upsertCurrentRates(rates: List<CurrentRateEntity>)

    @Query(
        "SELECT * FROM ${RateHistoryEntity.TABLE_NAME} " +
            "WHERE epoch_day >= :fromEpochDay ORDER BY epoch_day ASC",
    )
    fun observeHistory(fromEpochDay: Long): Flow<List<RateHistoryEntity>>

    /** Último cierre anterior a hoy para una fuente: alimenta la flecha de tendencia. */
    @Query(
        "SELECT * FROM ${RateHistoryEntity.TABLE_NAME} " +
            "WHERE source = :source AND epoch_day < :epochDay " +
            "ORDER BY epoch_day DESC LIMIT 1",
    )
    suspend fun previousClose(source: String, epochDay: Long): RateHistoryEntity?

    @Upsert
    suspend fun upsertHistory(points: List<RateHistoryEntity>)

    @Query("DELETE FROM ${RateHistoryEntity.TABLE_NAME} WHERE epoch_day < :beforeEpochDay")
    suspend fun pruneHistory(beforeEpochDay: Long): Int
}
