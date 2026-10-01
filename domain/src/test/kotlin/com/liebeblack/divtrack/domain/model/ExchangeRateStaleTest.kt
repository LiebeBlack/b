package com.liebeblack.divtrack.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Detección de "banco que dejó de publicar": la fuente responde 200 con JSON perfecto
 * sirviendo el cierre de ayer. Es exactamente el caso que se quiere destapar.
 */
class ExchangeRateStaleTest {

    private val now = STALE_HOURS * MILLIS_PER_HOUR + 10L

    @Test
    fun `el oficial mas viejo que su umbral es rancio`() {
        val rate = rate(RateSource.OFICIAL, ageMillis = now)

        assertTrue(rate.isStale(nowMillis = now))
    }

    @Test
    fun `el oficial dentro del umbral no es rancio`() {
        val rate = rate(RateSource.OFICIAL, ageMillis = STALE_HOURS * MILLIS_PER_HOUR - 1)

        assertFalse(rate.isStale(nowMillis = now))
    }

    @Test
    fun `el paralelo tiene un umbral mas estricto`() {
        // Viejo para el paralelo (12 h) pero fresco para el oficial (24 h).
        val rate = rate(RateSource.PARALELO, ageMillis = STALE_HOURS * MILLIS_PER_HOUR - 1)

        assertTrue(rate.isStale(nowMillis = now))
    }

    @Test
    fun `sin marca de tiempo no se supone rancio`() {
        val rate = rate(RateSource.OFICIAL, ageMillis = null)

        // Yadio no publica marca: suponer rancio mostraría un aviso falso.
        assertFalse(rate.isStale(nowMillis = now))
    }

    @Test
    fun `el paralelo dentro de su umbral no es rancio`() {
        val rate = rate(RateSource.PARALELO, ageMillis = 2L * MILLIS_PER_HOUR)

        assertFalse(rate.isStale(nowMillis = now))
    }

    private fun rate(source: RateSource, ageMillis: Long?): ExchangeRate = ExchangeRate(
        source = source,
        value = 860.18,
        previousClose = null,
        providerId = "DolarAPI",
        updatedAtMillis = ageMillis?.let { now - it },
        fetchedAtMillis = 0L,
    )

    private companion object {
        const val STALE_HOURS = 24L
        const val MILLIS_PER_HOUR = 3_600_000L
    }
}
