package com.liebeblack.divtrack.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta "terminal financiera": oscuro profundo con acentos saturados. Se prefieren
 * superficies planas con bordes finos antes que sombras: se ve más premium y cuesta menos
 * en gama baja (sin elevación que renderizar).
 */

// --- Superficies (dark-first) ---
val Obsidian = Color(0xFF0A0E13)
val ObsidianElevated = Color(0xFF131A22)
val ObsidianBorder = Color(0xFF223040)
val ObsidianMuted = Color(0xFF98A2B3)

// --- Superficies (light) ---
val CloudWhite = Color(0xFFF7F9FC)
val CloudSurface = Color(0xFFFFFFFF)
val CloudBorder = Color(0xFFD8DEE7)
val CloudShade = Color(0xFFE9EDF3)
val SlateText = Color(0xFF44505F)

// --- Acentos de marca ---
val Mint = Color(0xFF19E39B)
val MintDeep = Color(0xFF00A86B)
val Sky = Color(0xFF4C8DFF)
val SkyDeep = Color(0xFF1F5FD0)
val Amber = Color(0xFFFFB020)

/** Texto principal sobre oscuro: más blanco que [CloudWhite] para máximo contraste. */
val Frost = Color(0xFFF5F8FB)

// --- Semánticos ---
val TrendUp = Color(0xFF16C784)
val TrendDown = Color(0xFFEA3943)
val TrendFlat = Color(0xFF8A94A6)
val OfficialAccent = Sky
val ParallelAccent = Amber
