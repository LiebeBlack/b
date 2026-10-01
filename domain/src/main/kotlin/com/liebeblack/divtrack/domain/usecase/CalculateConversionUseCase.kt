package com.liebeblack.divtrack.domain.usecase

import com.liebeblack.divtrack.core.common.utils.AppConstants
import com.liebeblack.divtrack.domain.model.Conversion
import com.liebeblack.divtrack.domain.model.ConversionDirection
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.core.common.utils.NumberParsing
import javax.inject.Inject

/**
 * Calculadora bidireccional. Aritmética pura, sin Android y sin estado: la UI la llama en
 * cada pulsación de tecla (el cálculo es instantáneo, no hay botón "Calcular").
 */
class CalculateConversionUseCase @Inject constructor() {

    /**
     * @param rate tasa vigente de [source] en Bs por USD. `null` o <= 0 desactiva el cálculo.
     * @param rawAmount texto tal cual lo escribió el usuario (se parsea con tolerancia es-VE).
     * @return `null` si todavía no hay monto utilizable o no hay tasa: la UI muestra guiones.
     */
    operator fun invoke(
        direction: ConversionDirection,
        source: RateSource,
        rate: Double?,
        rawAmount: String,
        igtfEnabled: Boolean,
    ): Conversion? {
        val amount = NumberParsing.parseAmountOrNull(rawAmount) ?: return null
        if (amount <= 0.0) return null

        val safeRate = rate?.takeIf { it > 0.0 && it.isFinite() } ?: return null
        val igtfRate = if (igtfEnabled) AppConstants.IGTF_RATE else 0.0

        return when (direction) {
            ConversionDirection.USD_TO_BS -> {
                val netUsd = amount
                val netBs = netUsd * safeRate
                val igtfBs = netBs * igtfRate
                Conversion(
                    direction = direction,
                    source = source,
                    rate = safeRate,
                    netUsd = netUsd,
                    netBs = netBs,
                    igtfEnabled = igtfEnabled,
                    igtfRate = igtfRate,
                    igtfBs = igtfBs,
                    totalBs = netBs + igtfBs,
                    totalUsd = netUsd + netUsd * igtfRate,
                )
            }

            ConversionDirection.BS_TO_USD -> {
                val totalBs = amount
                val totalUsd = totalBs / safeRate
                val netUsd = if (igtfRate > 0.0) totalUsd / (1.0 + igtfRate) else totalUsd
                val netBs = netUsd * safeRate
                Conversion(
                    direction = direction,
                    source = source,
                    rate = safeRate,
                    netUsd = netUsd,
                    netBs = netBs,
                    igtfEnabled = igtfEnabled,
                    igtfRate = igtfRate,
                    igtfBs = totalBs - netBs,
                    totalBs = totalBs,
                    totalUsd = totalUsd,
                )
            }
        }
    }
}
