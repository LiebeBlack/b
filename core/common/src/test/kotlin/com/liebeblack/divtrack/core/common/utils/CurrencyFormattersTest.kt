package com.liebeblack.divtrack.core.common.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormattersTest {

    @Test
    fun `los bolivares usan punto de miles y coma decimal`() {
        assertEquals("859,06 Bs.", CurrencyFormatters.bolivars(859.0629))
        assertEquals("1.234,50 Bs.", CurrencyFormatters.bolivars(1234.5))
    }

    @Test
    fun `el importe sin sufijo no lleva moneda`() {
        assertEquals("954,55", CurrencyFormatters.amount(954.5512))
    }

    @Test
    fun `los porcentajes llevan signo explicito`() {
        assertEquals("+11,15 %", CurrencyFormatters.percent(11.1545))
        assertEquals("-2,04 %", CurrencyFormatters.percent(-2.04))
        assertEquals("0,00 %", CurrencyFormatters.percent(0.0))
    }

    @Test
    fun `el porcentaje sin signo se usa en etiquetas`() {
        assertEquals("5,00 %", CurrencyFormatters.percent(5.0, withSign = false))
    }
}
