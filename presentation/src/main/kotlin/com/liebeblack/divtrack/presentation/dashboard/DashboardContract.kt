package com.liebeblack.divtrack.presentation.dashboard

import com.liebeblack.divtrack.presentation.common.UiText

/**
 * Intenciones del usuario. MVI estricto: la UI nunca muta estado, solo describe lo que pasó.
 */
sealed interface DashboardIntent {

    /** Pull-to-refresh o botón de la barra superior. */
    data object Refresh : DashboardIntent

    /** Reintento desde el estado de error (dato vacío + sin conexión). */
    data object Retry : DashboardIntent
}

/** Eventos de un solo uso (nunca estado persistente). */
sealed interface DashboardEffect {

    data class ShowMessage(val text: UiText) : DashboardEffect
}
