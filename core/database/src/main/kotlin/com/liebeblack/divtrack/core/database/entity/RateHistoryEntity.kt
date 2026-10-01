package com.liebeblack.divtrack.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Cierre diario de cada tasa. La clave compuesta `(source, epoch_day)` hace que reescribir el
 * día en curso sea idempotente: nunca duplica días.
 *
 * Se usa `epochDay` (días desde 1970 en hora de Venezuela) en lugar de cadenas de fecha:
 * ocupa menos, ordena naturalmente y evita zonas horarias en SQL.
 *
 * **Qué queda de esta tabla y por qué.** La pantalla de Histórico se eliminó del producto
 * (con ella el gráfico, los rangos, las estadísticas y la importación de series diarias).
 * Estas dos filas por día (una por fuente) siguen existiendo por una única razón: son el
 * "cierre anterior" con el que el panel decide si la tasa sube, baja o se queda igual.
 *
 * El nombre de la clase y el de la tabla se conservan a propósito aunque el concepto ya no
 * sea "histórico": cambiarlos obligaría a una migración de Room, y en una app financiera
 * perder datos en silencio no es una opción (ver ADR 16).
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
