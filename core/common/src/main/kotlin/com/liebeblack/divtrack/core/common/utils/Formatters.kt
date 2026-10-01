package com.liebeblack.divtrack.core.common.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formateo de moneda y fechas con la convención venezolana (1.234,56).
 *
 * **Nota de rendimiento (y por qué está escrito así).** `DecimalFormat` no es thread-safe, y
 * la tentación es crear uno nuevo en cada llamada. Ese camino, en una app que formatea la
 * tasa en cada emisión de Room y en cada tecla de la calculadora, significa decenas de
 * instancias por segundo: analizar el patrón, clonar los símbolos y construir el objeto son
 * operaciones caras que no dependen del valor formateado.
 *
 * Aquí hay **un formateador por hilo y por formato** (nada compartido entre hilos, así que
 * sigue siendo seguro) y los `DateTimeFormatter`, que sí son inmutables y thread-safe, se
 * construyen una sola vez para toda la app.
 */
object CurrencyFormatters {

    private val locale: Locale = Locale.forLanguageTag(AppConstants.LOCALE_TAG)
    private val zone: ZoneId = ZoneId.of(AppConstants.TIME_ZONE_ID)

    private val timestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM · HH:mm", locale).withZone(zone)

    private val groupedFormatters = formatterCache()
    private val plainFormatters = formatterCache()

    /** "859,06" */
    fun amount(value: Double, decimals: Int = 2): String =
        formatter(decimals, grouping = true).format(value)

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
    fun timestamp(instant: Instant): String = timestampFormatter.format(instant)

    private fun formatterCache(): ThreadLocal<MutableMap<Int, DecimalFormat>> =
        object : ThreadLocal<MutableMap<Int, DecimalFormat>>() {
            override fun initialValue(): MutableMap<Int, DecimalFormat> = mutableMapOf()
        }

    private fun formatter(decimals: Int, grouping: Boolean): DecimalFormat {
        val cache: MutableMap<Int, DecimalFormat> =
            if (grouping) groupedFormatters.get() else plainFormatters.get()
        return cache.getOrPut(decimals) { buildFormatter(decimals, grouping) }
    }

    private fun buildFormatter(decimals: Int, grouping: Boolean): DecimalFormat {
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
