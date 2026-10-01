package com.liebeblack.divtrack.presentation.common

import android.content.Context

/**
 * Texto de UI que puede ser literal o una referencia a recursos.
 *
 * El ViewModel nunca toca `Context`: emite [Res]/[ResArgs] y la capa de Compose lo resuelve.
 * Así los eventos de un solo uso (snackbars) siguen siendo testeables en JVM pura.
 */
sealed interface UiText {

    data class Raw(val value: String) : UiText

    data class Res(val id: Int) : UiText

    /**
     * Recurso con argumentos de formato (`%1$d`, `%1$s`).
     *
     * Existe porque un error de red sin su código no es diagnosticable: "la fuente está
     * fallando (HTTP 503)" y "la fuente está fallando" se ven igual en pantalla, pero solo
     * el primero permite saber si el problema es del proveedor o de la app.
     */
    data class ResArgs(val id: Int, val args: List<Any>) : UiText
}

/** Resolución fuera de composición (corrutinas de efectos). */
fun UiText.asString(context: Context): String = when (this) {
    is UiText.Raw -> value
    is UiText.Res -> context.getString(id)
    is UiText.ResArgs -> context.getString(id, *args.toTypedArray())
}
