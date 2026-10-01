package com.liebeblack.divtrack.core.network.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parseo de los JSON **reales** capturados de cada proveedor (verificados en vivo contra
 * los endpoints). Cada fuente habla su propio dialecto; si alguno cambia de formato, el
 * test que lo fija se rompe y lo vemos antes que el usuario.
 */
class ProviderDtoParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Test
    fun `dolarapi - lista con promedio para oficial y paralelo`() {
        val payload = """
            [
              {
                "moneda": "USD", "fuente": "oficial", "nombre": "Dólar",
                "compra": null, "venta": null, "promedio": 860.1753,
                "fechaActualizacion": "2026-10-01T00:00:00-04:00"
              },
              {
                "moneda": "USD", "fuente": "paralelo", "nombre": "Paralelo",
                "compra": null, "venta": null, "promedio": 955.706247,
                "fechaActualizacion": "2026-10-01T12:01:34.203Z"
              }
            ]
        """.trimIndent()

        val dtos = json.decodeFromString<List<DolarApiRateDto>>(payload)

        assertEquals(2, dtos.size)
        assertEquals(860.1753, dtos[0].usableValue!!, 0.000001)
        assertEquals(955.706247, dtos[1].usableValue!!, 0.000001)
        assertFalse(dtos[0].fechaActualizacion.isNullOrEmpty())
    }

    @Test
    fun `dolarapi - degrada promedio luego venta luego compra`() {
        val soloVenta = json.decodeFromString<DolarApiRateDto>(
            """{ "fuente": "oficial", "compra": null, "venta": 100.5, "promedio": null }""",
        )
        val soloCompra = json.decodeFromString<DolarApiRateDto>(
            """{ "fuente": "oficial", "compra": 99.5, "venta": null, "promedio": null }""",
        )

        assertEquals(100.5, soloVenta.usableValue!!, 0.000001)
        assertEquals(99.5, soloCompra.usableValue!!, 0.000001)
    }

    @Test
    fun `yadio - mapa de divisas dentro de USD`() {
        val payload = """
            { "BTC": 83953.59,
              "USD": { "BTC": 0.000011911343, "EUR": 0.885131, "VES": 954.55 } }
        """.trimIndent()

        val dto = json.decodeFromString<YadioRatesDto>(payload)

        assertEquals(954.55, dto.ves!!, 0.000001)
    }

    @Test
    fun `yadio - sin par VES devuelve nulo en vez de romper`() {
        val dto = json.decodeFromString<YadioRatesDto>("""{ "USD": { "EUR": 0.885131 } }""")

        assertNull(dto.ves)
    }

    @Test
    fun `er-api - envoltorio con mapa plano y marca de tiempo en segundos`() {
        val payload = """
            { "result": "success",
              "time_last_update_unix": 1790812951,
              "base_code": "USD",
              "rates": { "USD": 1, "EUR": 0.881813, "VES": 860.1753 } }
        """.trimIndent()

        val dto = json.decodeFromString<ExchangeRateApiDto>(payload)

        assertFalse(dto.isFailure)
        assertEquals(860.1753, dto.rates[ExchangeRateApiDto.VES_KEY]!!, 0.000001)
        assertEquals(1_790_812_951_000L, dto.timeLastUpdateUnix!! * ExchangeRateApiDto.SECONDS_TO_MILLIS)
    }

    @Test
    fun `er-api - envoltorio de error se detecta sin explotar`() {
        val payload = """{ "result": "error", "error-type": "unsupported-code" }"""

        val dto = json.decodeFromString<ExchangeRateApiDto>(payload)

        assertTrue(dto.isFailure)
    }

    @Test
    fun `er-api - sin campo result no filtra (defensivo)`() {
        val dto = json.decodeFromString<ExchangeRateApiDto>("""{ "rates": { "VES": 860.0 } }""")

        assertFalse(dto.isFailure)
    }
}
