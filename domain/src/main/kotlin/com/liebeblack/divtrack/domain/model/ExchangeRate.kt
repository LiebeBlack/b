package com.liebeblack.divtrack.domain.model

import com.liebeblack.divtrack.core.common.utils.AppConstants

/** Dirección de la variación respecto al cierre anterior. */
enum class TrendDirection { UP, DOWN, FLAT }

/**
 * Tasa de cambio con su contexto de mercado.
 *
 * @param value valor vigente en Bs por USD.
 * @param previousClose último cierre anterior a hoy (hora de Venezuela); `null` mientras la
 * app no tenga guardado ningún cierre de un día anterior.
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

    /**
     * `true` cuando la marca de tiempo del proveedor es más vieja que el umbral de
     * frescura de esta fuente. Es la señal de "el banco dejó de publicar": DolarAPI puede
     * responder 200 con JSON perfecto sirviendo el cierre de ayer.
     *
     * Sin marca de tiempo (Yadio) no se supone rancio: no se muestra falso aviso.
     */
    fun isStale(nowMillis: Long): Boolean {
        val updatedAt = updatedAtMillis ?: return false
        val thresholdHours = when (source) {
            RateSource.OFICIAL -> AppConstants.STALE_RATE_HOURS
            RateSource.PARALELO -> AppConstants.STALE_PARALLEL_HOURS
        }
        return nowMillis - updatedAt > thresholdHours * MILLIS_PER_HOUR
    }

    private companion object {
        const val PERCENT_FACTOR = 100.0
        const val MILLIS_PER_HOUR = 3_600_000L
    }
}
