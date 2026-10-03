package com.liebeblack.divtrack.core.network.mapper

import com.liebeblack.divtrack.core.common.utils.AppConstants
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Parseo defensivo de timestamps y fechas ISO-8601.
 *
 * Los proveedores mezclan formatos: DolarAPI publica `2026-09-30T00:00:00-04:00` para el
 * oficial y `2026-09-30T21:01:20.140Z` para el paralelo. Se intentan todas las variantes
 * razonables y, si nada encaja, se devuelve `null` en vez de romper la sincronización.
 */
internal object IsoParsers {

    /** Hora legal de Venezuela: la misma zona con la que la app pinta las fechas. */
    private val venezuelaZone: ZoneId = ZoneId.of(AppConstants.TIME_ZONE_ID)

    fun instantMillisOrNull(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null

        return runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .recoverCatching { Instant.parse(value).toEpochMilli() }
            .recoverCatching {
                // Solo fecha, sin hora ni zona: es una fecha local del proveedor, así que se
                // lee como medianoche de Venezuela. Con UTC, "2026-09-30" se pintaría en la
                // app como 29 sep (la UI muestra en hora local): un día antes del publicado.
                LocalDate.parse(value)
                    .atStartOfDay(venezuelaZone)
                    .toInstant()
                    .toEpochMilli()
            }
            .getOrNull()
    }
}
