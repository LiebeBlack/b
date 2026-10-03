package com.liebeblack.divtrack.presentation.theme

import androidx.compose.ui.unit.dp

/** Escala de espaciado única: nada de números mágicos repartidos por las pantallas. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp

    // `xxl` (32 dp) existía solo para el estado vacío que se eliminó por inalcanzable; si
    // vuelve a hacer falta, se añade aquí en una línea (ADR 19).
}
