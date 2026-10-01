package com.liebeblack.divtrack.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET https://api.yadio.io/exrates/USD`
 *
 * Respuesta real verificada (mapa de divisas):
 * ```json
 * { "BTC": 83531.76, "USD": { "EUR": 0.88, "...": 0.0, "VES": 954.55 } }
 * ```
 * Yadio es la fuente upstream que publica el paralelo, así que sirve de fallback natural
 * cuando DolarAPI no responde. Solo se consume `USD["VES"]`; el resto se ignora.
 */
@Serializable
data class YadioRatesDto(
    @SerialName("BTC") val btc: Double? = null,
    @SerialName("USD") val usd: Map<String, Double> = emptyMap(),
) {
    val ves: Double? get() = usd[VES_KEY]

    companion object {
        const val VES_KEY: String = "VES"
    }
}
