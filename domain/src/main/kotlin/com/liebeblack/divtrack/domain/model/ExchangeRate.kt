package com.liebeblack.divtrack.domain.model

/** Dirección de la variación respecto al cierre anterior. */
enum class TrendDirection { UP, DOWN, FLAT }

/**
 * Tasa de cambio con su contexto de mercado.
 *
 * @param value valor vigente en Bs por USD.
 * @param previousClose último cierre anterior a hoy (hora de Venezuela); `null` cuando
 * todavía no hay histórico suficiente.
 * @param providerId API que resolvió la tasa: la UI muestra la procedencia.
 * @param updatedAtMillis marca de tiempo publicada por el proveedor (puede no existir).
 * @param fetchedAtMillis cuándo la app obtuvo el dato.
 */
data class ExchangeRate(
    val source: RateSource,
    val value: Double,
    val previousClose: Double?,
    val providerId: String,
    val updatedAtMillis: Long?,
    val fetchedAtMillis: Long,
) {
    val changeAbsolute: Double?
        get() = previousClose?.let { value - it }

    val changePercent: Double?
        get() = previousClose
            ?.takeIf { it != 0.0 }
            ?.let { previous -> (value - previous) / previous * PERCENT_FACTOR }

    val trend: TrendDirection
        get() {
            val previous = previousClose ?: return TrendDirection.FLAT
            return when {
                value > previous -> TrendDirection.UP
                value < previous -> TrendDirection.DOWN
                else -> TrendDirection.FLAT
            }
        }

    private companion object {
        const val PERCENT_FACTOR = 100.0
    }
}
