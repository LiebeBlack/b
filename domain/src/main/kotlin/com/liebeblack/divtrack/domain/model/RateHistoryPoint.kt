package com.liebeblack.divtrack.domain.model

import java.time.LocalDate

/**
 * Punto de la serie diaria. Se guarda como `epochDay` (entero) en lugar de `LocalDate`:
 * ocupa menos, ordena naturalmente en SQL y no arrastra zona horaria a la base de datos.
 */
data class RateHistoryPoint(
    val source: RateSource,
    val epochDay: Long,
    val value: Double,
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
}
