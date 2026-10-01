package com.liebeblack.divtrack.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryRangeTest {

    private val today = LocalDate.of(2026, 10, 1)

    @Test
    fun `ytd arranca el primero de enero`() {
        assertEquals(LocalDate.of(2026, 1, 1), HistoryRange.YEAR_TO_DATE.since(today))
    }

    @Test
    fun `un mes atras y tres meses atras`() {
        assertEquals(LocalDate.of(2026, 9, 1), HistoryRange.ONE_MONTH.since(today))
        assertEquals(LocalDate.of(2026, 7, 1), HistoryRange.THREE_MONTHS.since(today))
    }

    @Test
    fun `un año atras`() {
        assertEquals(LocalDate.of(2025, 10, 1), HistoryRange.ONE_YEAR.since(today))
    }

    @Test
    fun `el rango por defecto es tres meses`() {
        assertEquals(HistoryRange.THREE_MONTHS, HistoryRange.default)
    }
}
