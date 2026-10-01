package com.liebeblack.divtrack.core.common.utils

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Parseo y saneado de importes escritos por una persona en Venezuela.
 *
 * La gente escribe indistintamente `1234,56`, `1.234,56`, `1,234.56`, `1234.56` o pega
 * montos con el símbolo de la moneda. En vez de obligar a un formato, el parser es tolerante
 * y el campo de texto se sanea a la vez que se teclea (sin botón "Calcular" ni sorpresas).
 */
object NumberParsing {

    /** Devuelve el valor numérico o `null` si el texto todavía no es un número usable. */
    fun parseAmountOrNull(raw: String): Double? {
        val cleaned = raw.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
        if (cleaned.isEmpty()) return null

        val negative = cleaned.startsWith("-")
        val body = cleaned.removePrefix("-")
        if (body.isEmpty()) return null

        val decimalIndex = decimalSeparatorIndex(body)

        val normalized = buildString {
            body.forEachIndexed { index, character ->
                when {
                    character.isDigit() -> append(character)
                    index == decimalIndex -> append('.')
                    else -> Unit // separador de miles: se descarta
                }
            }
        }

        if (normalized.isEmpty() || normalized == ".") return null
        val value = normalized.toDoubleOrNull() ?: return null
        return if (negative) -value else value
    }

    /**
     * Sanea lo que el usuario teclea o pega: deja un único separador decimal (coma),
     * como máximo [maxDecimals] decimales y descarta el resto de separadores de miles.
     * Mantiene la coma final para poder seguir escribiendo decimales.
     */
    fun sanitizeAmountInput(raw: String, maxDecimals: Int = 2): String {
        val cleaned = raw.filter { it.isDigit() || it == '.' || it == ',' }
        if (cleaned.isEmpty()) return ""

        val decimalIndex = decimalSeparatorIndex(cleaned)

        val integerDigits = (if (decimalIndex >= 0) cleaned.substring(0, decimalIndex) else cleaned)
            .filter { it.isDigit() }
            .trimStart('0')
            .ifEmpty { "0" }

        if (decimalIndex < 0) return integerDigits

        val decimals = (cleaned.substring(decimalIndex + 1))
            .filter { it.isDigit() }
            .take(maxDecimals)

        return "$integerDigits,$decimals"
    }

    /** Convierte un valor calculado en texto editable (sin separador de miles). */
    fun formatForInput(value: Double, maxDecimals: Int = 2): String {
        if (!value.isFinite()) return ""
        val rounded = BigDecimal.valueOf(value)
            .setScale(maxDecimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        return rounded.toPlainString().replace('.', ',')
    }

    /**
     * Índice del separador decimal dentro de [body], o -1 si todos los separadores son de miles.
     *
     * Reglas (documentadas porque la ambigüedad es real):
     * - Si conviven `.` y `,`, el último en aparecer es el decimal ("1.234,56" y "1,234.56").
     * - Si solo hay uno y le siguen exactamente 3 dígitos en un número de más de 3 dígitos,
     *   se interpreta como separador de miles ("1.234" -> 1234), la convención local.
     */
    private fun decimalSeparatorIndex(body: String): Int {
        val lastDot = body.lastIndexOf('.')
        val lastComma = body.lastIndexOf(',')

        return when {
            lastDot >= 0 && lastComma >= 0 -> maxOf(lastDot, lastComma)
            lastDot >= 0 -> if (looksLikeDecimal(body, lastDot)) lastDot else -1
            lastComma >= 0 -> if (looksLikeDecimal(body, lastComma)) lastComma else -1
            else -> -1
        }
    }

    private fun looksLikeDecimal(body: String, separatorIndex: Int): Boolean {
        val decimals = body.length - separatorIndex - 1
        val separatorCount = body.count { it == '.' || it == ',' }
        if (separatorCount > 1) return false
        // "1.234" (miles) vs "12.5" (decimal) vs "1.5" (decimal)
        return decimals != 3 || body.length <= 3
    }
}
