package com.liebeblack.divtrack.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tasa vigente por fuente. Una fila por `oficial` / `paralelo` (la clave es la fuente),
 * así que el `upsert` es idempotente y la UI siempre ve exactamente dos tarjetas.
 */
@Entity(tableName = CurrentRateEntity.TABLE_NAME)
data class CurrentRateEntity(
    @PrimaryKey
    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "value")
    val value: Double,

    /** Cierre del día anterior en hora de Venezuela: base de la flecha de tendencia. */
    @ColumnInfo(name = "previous_close")
    val previousClose: Double?,

    /** Proveedor que resolvió esta tasa (DolarAPI, Yadio, ...). */
    @ColumnInfo(name = "provider_id")
    val providerId: String,

    /** Marca de tiempo que publica el proveedor (puede ser null). */
    @ColumnInfo(name = "updated_at")
    val updatedAtMillis: Long?,

    /** Cuándo la app obtuvo el dato. */
    @ColumnInfo(name = "fetched_at")
    val fetchedAtMillis: Long,
) {
    companion object {
        const val TABLE_NAME: String = "current_rates"
    }
}
