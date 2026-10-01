package com.liebeblack.divtrack.domain.model

/** Sentido de la conversión en la calculadora. */
enum class ConversionDirection { USD_TO_BS, BS_TO_USD }

/**
 * Resultado de una conversión bidireccional.
 *
 * Semántica del IGTF (3 % sobre pagos en divisas), documentada porque es la parte donde
 * es fácil equivocarse:
 *
 * - [ConversionDirection.USD_TO_BS]: el usuario escribe dólares. `netUsd` es el monto
 *   tecleado, `netBs = netUsd * rate` y el IGTF se suma encima:
 *   `totalBs = netBs + netBs * igtfRate`. Ejemplo del spec: $10 a 36,5 Bs con IGTF ->
 *   netBs = 365,00, igtfBs = 10,95, totalBs = 375,95 Bs.
 *
 * - [ConversionDirection.BS_TO_USD]: el usuario escribe bolívares como importe total a
 *   pagar. `totalBs` es lo tecleado, `totalUsd = totalBs / rate` y el neto que recibe el
 *   comercio se descuenta del IGTF: `netUsd = totalUsd / (1 + igtfRate)`.
 *
 * Todos los importes van sin redondear: el redondeo es una decisión de presentación.
 */
data class Conversion(
    val direction: ConversionDirection,
    val source: RateSource,
    val rate: Double,
    val netUsd: Double,
    val netBs: Double,
    val igtfEnabled: Boolean,
    val igtfRate: Double,
    val igtfBs: Double,
    val totalBs: Double,
    val totalUsd: Double,
) {
    val hasAmount: Boolean get() = totalBs > 0.0 || totalUsd > 0.0
}
