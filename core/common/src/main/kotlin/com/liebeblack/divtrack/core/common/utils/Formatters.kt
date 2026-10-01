package com.liebeblack.divtrack.core.common.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formateo de moneda y fechas con la convención venezolana (1.234,56).
 *
 * Nota de rendimiento: [DecimalFormat] no es thread-safe, así que se crea por llamada.
 * En Compose se invoca siempre dentro de `remember(...)` o en el ViewModel, nunca en el
 * bucle de dibujo de un Canvas.
 */
object CurrencyFormatters {

    private val locale: Locale = Locale.forLanguageTag(AppConstants.LOCALE_TAG)
    private val zone: ZoneId = ZoneId.of(AppConstants.TIME_ZONE_ID)

    /** "859,06" */
    fun amount(value: Double, decimals: Int = 2): String = formatter(decimals, grouping = true).format(value)

    /** "859,06 Bs." */
    fun bolivars(value: Double, decimals: Int = 2): String = "${amount(value, decimals)} Bs."

    /** "+11,15 %" / "-2,04 %" */
    fun percent(value: Double, decimals: Int = 2, withSign: Boolean = true): String {
        val formatted = formatter(decimals, grouping = false).format(kotlin.math.abs(value))
        val sign = when {
            !withSign -> ""
            value > 0.0 -> "+"
            value < 0.0 -> "-"
            else -> ""
        }
        return "$sign$formatted %"
    }

    /** "30 sep · 21:01" en hora de Venezuela. */
    fun timestamp(instant: Instant): String =
        DateTimeFormatter.ofPattern("d MMM · HH:mm", locale).withZone(zone).format(instant)

    /** "30 sep" */
    fun shortDate(date: LocalDate): String =
        DateTimeFormatter.ofPattern("d MMM", locale).format(date)

    private fun formatter(decimals: Int, grouping: Boolean): DecimalFormat {
        val symbols = DecimalFormatSymbols(locale).apply {
            decimalSeparator = ','
            groupingSeparator = '.'
            minusSign = '-'
        }
        val pattern = buildString {
            if (grouping) append("#,##0") else append("0")
            if (decimals > 0) {
                append('.')
                repeat(decimals) { append('0') }
            }
        }
        return DecimalFormat(pattern, symbols)
    }
}
