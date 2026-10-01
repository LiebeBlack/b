package com.liebeblack.divtrack.core.network.dto

import kotlinx.serialization.Serializable

/**
 * `GET https://ve.dolarapi.com/v1/historicos/dolares/{fuente}`
 *
 * Respuesta real verificada:
 * ```json
 * [ { "fuente":"oficial", "compra":null, "venta":null,
 *     "promedio":17.5591, "fecha":"2023-01-03" } ]
 * ```
 * Es la base del gráfico YTD: un punto por día, desde 2023.
 */
@Serializable
data class DolarApiHistoryDto(
    val fuente: String,
    val compra: Double? = null,
    val venta: Double? = null,
    val promedio: Double? = null,
    val fecha: String,
) {
    val usableValue: Double?
        get() = promedio ?: venta ?: compra
}
