package com.liebeblack.divtrack.core.network.dto

import kotlinx.serialization.Serializable

/**
 * `GET https://open.er-api.com/v6/latest/USD`
 *
 * Respuesta real verificada (2026-10-01):
 * ```json
 * { "result":"success", "time_last_update_unix":1790812951,
 *   "base_code":"USD",
 *   "rates": { "USD": 1, ..., "VES": 860.1753, ... } }
 * ```
 *
 * Tercer estilo de lectura del proyecto, distinto a propósito de los otros dos:
 * - DolarAPI publica una **lista** con un elemento por tasa (`fuente` + `promedio`).
 * - Yadio publica **un mapa** de divisas dentro de `USD`.
 * - ExchangeRate-API publica un **envoltorio de estado** (`result`) con metadatos de
 *   actualización y el mapa `rates` plano al nivel raíz.
 *
 * El par VES no existe o es <= 0: el provider lo descarta en lugar de inventar un cero.
 */
@Serializable
data class ExchangeRateApiDto(
    /** Estado del envoltorio: `"success"` o `"error"`. Vacío = no filtra (defensivo). */
    val result: String? = null,

    /** Última actualización del proveedor, en segundos Unix (no milisegundos). */
    @kotlinx.serialization.SerialName("time_last_update_unix")
    val timeLastUpdateUnix: Long? = null,

    @kotlinx.serialization.SerialName("base_code")
    val baseCode: String? = null,

    val rates: Map<String, Double> = emptyMap(),
) {
    /** `true` cuando el envoltorio dice error de forma explícita. */
    val isFailure: Boolean get() = result != null && !result.equals(SUCCESS, ignoreCase = true)

    companion object {
        const val SUCCESS: String = "success"
        const val VES_KEY: String = "VES"

        /** El proveedor publica la marca de tiempo en segundos; la app trabaja en ms. */
        const val SECONDS_TO_MILLIS: Long = 1_000L
    }
}
