package com.liebeblack.divtrack.presentation.common

import android.content.Context

/**
 * Texto de UI que puede ser literal o una referencia a recursos.
 *
 * El ViewModel nunca toca `Context`: emite [Res] y la capa de Compose lo resuelve. Así los
 * eventos de un solo uso (snackbars) siguen siendo testeables en JVM pura.
 */
sealed interface UiText {

    data class Raw(val value: String) : UiText

    data class Res(val id: Int) : UiText
}

/** Resolución fuera de composición (corrutinas de efectos). */
fun UiText.asString(context: Context): String = when (this) {
    is UiText.Raw -> value
    is UiText.Res -> context.getString(id)
}
