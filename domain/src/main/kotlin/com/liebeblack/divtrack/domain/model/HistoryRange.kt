package com.liebeblack.divtrack.domain.model

import java.time.LocalDate

/**
 * Rangos disponibles en el gráfico histórico. El cálculo de la fecha de corte vive en el
 * dominio (no en la UI) porque depende del día actual en hora de Venezuela.
 */
enum class HistoryRange {
    ONE_MONTH,
    THREE_MONTHS,
    YEAR_TO_DATE,
    ONE_YEAR;

    fun since(today: LocalDate): LocalDate = when (this) {
        ONE_MONTH -> today.minusMonths(1)
        THREE_MONTHS -> today.minusMonths(3)
        YEAR_TO_DATE -> LocalDate.of(today.year, 1, 1)
        ONE_YEAR -> today.minusYears(1)
    }

    companion object {
        val default: HistoryRange = THREE_MONTHS
    }
}
