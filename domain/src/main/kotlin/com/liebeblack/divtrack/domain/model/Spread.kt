package com.liebeblack.divtrack.domain.model

/**
 * Diferencia entre el paralelo y el oficial, el indicador que de verdad importa en
 * Venezuela: cuánto más caro sale el dólar de mercado respecto al oficial.
 */
data class Spread(
    val oficial: Double?,
    val paralelo: Double?,
    val absolute: Double?,
    val percent: Double?,
) {
    val isAvailable: Boolean get() = absolute != null && percent != null

    companion object {
        fun calculate(oficial: Double?, paralelo: Double?): Spread {
            if (oficial == null || paralelo == null || oficial <= 0.0) {
                return Spread(oficial = oficial, paralelo = paralelo, absolute = null, percent = null)
            }
            val absolute = paralelo - oficial
            return Spread(
                oficial = oficial,
                paralelo = paralelo,
                absolute = absolute,
                percent = absolute / oficial * PERCENT_FACTOR,
            )
        }

        private const val PERCENT_FACTOR = 100.0
    }
}
