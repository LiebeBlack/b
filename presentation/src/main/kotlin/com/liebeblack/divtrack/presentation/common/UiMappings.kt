package com.liebeblack.divtrack.presentation.common

import androidx.annotation.StringRes
import com.liebeblack.divtrack.domain.model.HistoryRange
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.ThemeMode
import com.liebeblack.divtrack.domain.model.TrendDirection
import com.liebeblack.divtrack.presentation.R

/**
 * Mapeos dominio -> recursos. Mantener los textos fuera del dominio es lo que permite
 * localizar la app (es-VE por defecto, inglés en `values-en`) sin tocar lógica.
 */

@StringRes
fun RateSource.labelRes(): Int = when (this) {
    RateSource.OFICIAL -> R.string.source_oficial
    RateSource.PARALELO -> R.string.source_paralelo
}

@StringRes
fun RateSource.longLabelRes(): Int = when (this) {
    RateSource.OFICIAL -> R.string.source_oficial_long
    RateSource.PARALELO -> R.string.source_paralelo_long
}

@StringRes
fun HistoryRange.labelRes(): Int = when (this) {
    HistoryRange.ONE_MONTH -> R.string.range_one_month
    HistoryRange.THREE_MONTHS -> R.string.range_three_months
    HistoryRange.YEAR_TO_DATE -> R.string.range_year_to_date
    HistoryRange.ONE_YEAR -> R.string.range_one_year
}

@StringRes
fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@StringRes
fun TrendDirection.contentDescriptionRes(): Int = when (this) {
    TrendDirection.UP -> R.string.trend_up
    TrendDirection.DOWN -> R.string.trend_down
    TrendDirection.FLAT -> R.string.trend_flat
}
