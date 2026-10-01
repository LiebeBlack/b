package com.liebeblack.divtrack.presentation.calculator

import android.content.Context
import com.liebeblack.divtrack.domain.model.ConversionDirection
import com.liebeblack.divtrack.presentation.R

/**
 * Construye el texto que se copia al portapapeles.
 *
 * Formato del spec (una línea resumen + detalle):
 * `DivTrack · Monto: $10,00 | Tasa Oficial: 36,50 Bs | IGTF: Sí | Total: 375,95 Bs`
 *
 * Se hace en la capa de UI y no en el ViewModel para poder usar recursos localizados
 * (es-VE / en) sin que el ViewModel toque `Context`.
 */
internal fun buildBreakdownText(context: Context, summary: BreakdownSummary): String {
    val sourceLabel = context.getString(
        when (summary.source) {
            com.liebeblack.divtrack.domain.model.RateSource.OFICIAL -> R.string.source_oficial_long
            com.liebeblack.divtrack.domain.model.RateSource.PARALELO -> R.string.source_paralelo_long
        },
    )
    val igtfLabel = context.getString(
        if (summary.igtfEnabled) R.string.copy_igtf_yes else R.string.copy_igtf_no,
    )
    val amountPrefix = if (summary.direction == ConversionDirection.USD_TO_BS) "$" else ""

    val header = context.getString(
        R.string.copy_summary,
        "$amountPrefix${summary.amountText}",
        sourceLabel,
        summary.rateText,
        igtfLabel,
        summary.totalBsText,
    )

    val detail = context.getString(
        R.string.copy_detail,
        sourceLabel,
        summary.netUsdText,
        summary.igtfText,
        summary.totalUsdText,
    )

    return buildString {
        append(header)
        append('\n')
        append(detail)
        append('\n')
        append(context.getString(R.string.copy_signature))
    }
}
