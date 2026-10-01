package com.liebeblack.divtrack.core.network.mapper

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.Instant

/**
 * Parseo defensivo de timestamps y fechas ISO-8601.
 *
 * Los proveedores mezclan formatos: DolarAPI publica `2026-09-30T00:00:00-04:00` para el
 * oficial y `2026-09-30T21:01:20.140Z` para el paralelo. Se intentan todas las variantes
 * razonables y, si nada encaja, se devuelve `null` en vez de romper la sincronización.
 */
internal object IsoParsers {

    fun instantMillisOrNull(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null

        return runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .recoverCatching { Instant.parse(value).toEpochMilli() }
            .recoverCatching {
                // Solo fecha: se interpreta como medianoche UTC, el valor se usa para mostrar.
                LocalDate.parse(value)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()
            }
            .getOrNull()
    }

    fun localDateOrNull(raw: String?): LocalDate? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        return runCatching { LocalDate.parse(value) }.getOrNull()
    }
}
