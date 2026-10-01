package com.liebeblack.divtrack.domain.usecase

import com.liebeblack.divtrack.domain.model.ConversionDirection
import com.liebeblack.divtrack.domain.model.RateSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * La corrección del IGTF es el punto donde una app financiera se gana o pierde la confianza
 * del usuario, así que se prueba con el ejemplo exacto del pliego: 10 USD a 36,5 Bs -> 375,95 Bs.
 */
class CalculateConversionUseCaseTest {

    private val useCase = CalculateConversionUseCase()

    @Test
    fun `dolares a bolivares con igtf suma el impuesto sobre el neto`() {
        val conversion = useCase(
            direction = ConversionDirection.USD_TO_BS,
            source = RateSource.OFICIAL,
            rate = 36.5,
            rawAmount = "10",
            igtfEnabled = true,
        )!!

        assertEquals(10.0, conversion.netUsd, DELTA)
        assertEquals(365.0, conversion.netBs, DELTA)
        assertEquals(10.95, conversion.igtfBs, DELTA)
        assertEquals(375.95, conversion.totalBs, DELTA)
    }

    @Test
    fun `dolares a bolivares sin igtf no altera el total`() {
        val conversion = useCase(
            direction = ConversionDirection.USD_TO_BS,
            source = RateSource.PARALELO,
            rate = 954.55,
            rawAmount = "100,50",
            igtfEnabled = false,
        )!!

        assertEquals(100.5 * 954.55, conversion.netBs, DELTA)
        assertEquals(0.0, conversion.igtfBs, DELTA)
        assertEquals(conversion.netBs, conversion.totalBs, DELTA)
    }

    @Test
    fun `bolivares a dolares con igtf descuenta el impuesto del neto`() {
        val conversion = useCase(
            direction = ConversionDirection.BS_TO_USD,
            source = RateSource.OFICIAL,
            rate = 36.5,
            rawAmount = "375,95",
            igtfEnabled = true,
        )!!

        assertEquals(375.95, conversion.totalBs, DELTA)
        assertEquals(10.0, conversion.netUsd, DELTA)
        assertEquals(365.0, conversion.netBs, DELTA)
    }

    @Test
    fun `sin monto no hay conversion`() {
        assertNull(
            useCase(ConversionDirection.USD_TO_BS, RateSource.OFICIAL, 36.5, "", igtfEnabled = true),
        )
        assertNull(
            useCase(ConversionDirection.USD_TO_BS, RateSource.OFICIAL, 36.5, "0", igtfEnabled = true),
        )
    }

    @Test
    fun `sin tasa no hay conversion`() {
        assertNull(
            useCase(ConversionDirection.USD_TO_BS, RateSource.OFICIAL, null, "10", igtfEnabled = true),
        )
        assertNull(
            useCase(ConversionDirection.USD_TO_BS, RateSource.OFICIAL, 0.0, "10", igtfEnabled = true),
        )
    }

    private companion object {
        const val DELTA = 0.000001
    }
}
