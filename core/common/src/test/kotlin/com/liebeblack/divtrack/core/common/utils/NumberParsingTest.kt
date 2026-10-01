package com.liebeblack.divtrack.core.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * El parser de importes es la pieza que hace que la calculadora se sienta "inteligente":
 * acepta lo que la gente escribe de verdad en Venezuela sin obligar a un formato.
 */
class NumberParsingTest {

    @Test
    fun `coma decimal se interpreta como decimal`() {
        assertEquals(12.5, NumberParsing.parseAmountOrNull("12,5"), DELTA)
    }

    @Test
    fun `punto de miles con coma decimal se interpreta correctamente`() {
        assertEquals(1_234.56, NumberParsing.parseAmountOrNull("1.234,56"), DELTA)
    }

    @Test
    fun `formato ingles tambien se acepta`() {
        assertEquals(1_234.56, NumberParsing.parseAmountOrNull("1,234.56"), DELTA)
    }

    @Test
    fun `punto con tres digitos se lee como separador de miles`() {
        assertEquals(1_234.0, NumberParsing.parseAmountOrNull("1.234"), DELTA)
    }

    @Test
    fun `punto con dos digitos se lee como decimal`() {
        assertEquals(36.5, NumberParsing.parseAmountOrNull("36.50"), DELTA)
    }

    @Test
    fun `texto sin numeros no produce valor`() {
        assertNull(NumberParsing.parseAmountOrNull(""))
        assertNull(NumberParsing.parseAmountOrNull("abc"))
        assertNull(NumberParsing.parseAmountOrNull("-"))
    }

    @Test
    fun `el saneado descarta separadores de miles y limita decimales`() {
        assertEquals("1234,56", NumberParsing.sanitizeAmountInput("1.234,56"))
        assertEquals("12,34", NumberParsing.sanitizeAmountInput("12,3456"))
        assertEquals("0", NumberParsing.sanitizeAmountInput("000"))
        assertEquals("", NumberParsing.sanitizeAmountInput(""))
    }

    @Test
    fun `el saneado conserva la coma final para poder seguir escribiendo`() {
        assertEquals("12,", NumberParsing.sanitizeAmountInput("12,"))
    }

    @Test
    fun `el formateo de entrada no usa separador de miles y es reversible`() {
        assertEquals("375,95", NumberParsing.formatForInput(375.95))
        assertEquals("100", NumberParsing.formatForInput(100.0))
        assertEquals(375.95, NumberParsing.parseAmountOrNull("375,95"), DELTA)
    }

    private companion object {
        const val DELTA = 0.000001
    }
}
