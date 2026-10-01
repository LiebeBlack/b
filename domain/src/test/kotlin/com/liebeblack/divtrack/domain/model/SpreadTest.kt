package com.liebeblack.divtrack.domain.model

import com.liebeblack.divtrack.domain.usecase.CalculateSpreadUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpreadTest {

    @Test
    fun `calcula diferencia absoluta y porcentual`() {
        val spread = Spread.calculate(oficial = 859.06, paralelo = 954.55)

        assertEquals(95.49, spread.absolute!!, DELTA)
        assertEquals(11.116, spread.percent!!, 0.01)
        assertTrue(spread.isAvailable)
    }

    @Test
    fun `sin oficial no hay brecha`() {
        val spread = Spread.calculate(oficial = null, paralelo = 954.55)

        assertNull(spread.absolute)
        assertNull(spread.percent)
        assertTrue(!spread.isAvailable)
    }

    @Test
    fun `oficial en cero no divide por cero`() {
        val spread = Spread.calculate(oficial = 0.0, paralelo = 954.55)

        assertNull(spread.percent)
    }

    @Test
    fun `el caso de uso toma las tasas de la lista`() {
        val rates = listOf(
            rate(RateSource.OFICIAL, 859.06),
            rate(RateSource.PARALELO, 954.55),
        )

        val spread = CalculateSpreadUseCase()(rates)

        assertEquals(95.49, spread.absolute!!, DELTA)
    }

    private fun rate(source: RateSource, value: Double) = ExchangeRate(
        source = source,
        value = value,
        previousClose = null,
        providerId = "test",
        updatedAtMillis = null,
        fetchedAtMillis = 0L,
    )

    private companion object {
        const val DELTA = 0.000001
    }
}
