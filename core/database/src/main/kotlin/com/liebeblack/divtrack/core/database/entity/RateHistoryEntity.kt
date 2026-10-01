package com.liebeblack.divtrack.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Serie diaria para el gráfico. La clave compuesta `(source, epoch_day)` hace que
 * reimportar el histórico del proveedor sea idempotente: nunca duplica días.
 *
 * Se usa `epochDay` (días desde 1970 en hora de Venezuela) en lugar de cadenas de fecha:
 * ocupa menos, ordena naturalmente y evita zonas horarias en SQL.
 */
@Entity(
    tableName = RateHistoryEntity.TABLE_NAME,
    primaryKeys = ["source", "epoch_day"],
    indices = [Index(value = ["epoch_day"], name = "index_rate_history_epoch_day")],
)
data class RateHistoryEntity(
    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "epoch_day")
    val epochDay: Long,

    @ColumnInfo(name = "value")
    val value: Double,

    @ColumnInfo(name = "provider_id")
    val providerId: String,
) {
    companion object {
        const val TABLE_NAME: String = "rate_history"
    }
}
