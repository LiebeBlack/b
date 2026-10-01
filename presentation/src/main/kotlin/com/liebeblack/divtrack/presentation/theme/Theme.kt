package com.liebeblack.divtrack.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.liebeblack.divtrack.domain.model.ThemeMode

/**
 * Colores que Material 3 no cubre: semántica de mercado y acentos por fuente.
 * Se exponen por `CompositionLocal` para que no haga falta pasar colores por parámetro
 * a cada componente (menos parámetros = menos recomposiciones).
 */
@Immutable
data class DivTrackColors(
    val trendUp: Color,
    val trendDown: Color,
    val trendFlat: Color,
    val officialAccent: Color,
    val parallelAccent: Color,
    val chartOficial: Color,
    val chartParalelo: Color,
)

private val DarkColorScheme = darkColorScheme(
    primary = Mint,
    onPrimary = Obsidian,
    primaryContainer = MintDeep,
    onPrimaryContainer = Obsidian,
    secondary = Sky,
    onSecondary = Obsidian,
    secondaryContainer = SkyDeep,
    onSecondaryContainer = CloudWhite,
    tertiary = Amber,
    onTertiary = Obsidian,
    background = Obsidian,
    onBackground = CloudWhite,
    surface = Obsidian,
    onSurface = CloudWhite,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = ObsidianMuted,
    surfaceContainer = ObsidianElevated,
    surfaceContainerHigh = ObsidianBorder,
    surfaceContainerLow = Obsidian,
    surfaceContainerLowest = Obsidian,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorder,
    error = TrendDown,
    onError = CloudWhite,
)

private val LightColorScheme = lightColorScheme(
    primary = MintDeep,
    onPrimary = CloudWhite,
    primaryContainer = Mint,
    onPrimaryContainer = Obsidian,
    secondary = SkyDeep,
    onSecondary = CloudWhite,
    secondaryContainer = Sky,
    onSecondaryContainer = Obsidian,
    tertiary = Amber,
    onTertiary = Obsidian,
    background = CloudWhite,
    onBackground = Obsidian,
    surface = CloudSurface,
    onSurface = Obsidian,
    surfaceVariant = CloudWhite,
    onSurfaceVariant = SlateText,
    surfaceContainer = CloudSurface,
    surfaceContainerHigh = CloudWhite,
    surfaceContainerLow = CloudSurface,
    surfaceContainerLowest = CloudWhite,
    outline = CloudBorder,
    outlineVariant = CloudBorder,
    error = TrendDown,
    onError = CloudWhite,
)

private val DarkExtraColors = DivTrackColors(
    trendUp = TrendUp,
    trendDown = TrendDown,
    trendFlat = TrendFlat,
    officialAccent = OfficialAccent,
    parallelAccent = ParallelAccent,
    chartOficial = Sky,
    chartParalelo = Amber,
)

private val LightExtraColors = DivTrackColors(
    trendUp = TrendUp,
    trendDown = TrendDown,
    trendFlat = TrendFlat,
    officialAccent = SkyDeep,
    parallelAccent = Amber,
    chartOficial = SkyDeep,
    chartParalelo = Amber,
)

val LocalDivTrackColors = staticCompositionLocalOf { DarkExtraColors }

/** Acceso al tema extendido: `DivTrackThemeTokens.colors.trendUp`. */
object DivTrackThemeTokens {
    val colors: DivTrackColors
        @Composable
        @ReadOnlyComposable
        get() = LocalDivTrackColors.current

}

/**
 * Tema único de la app. `dynamicColor` se deja fuera a propósito: la identidad visual es
 * parte del producto, no del wallpaper del usuario.
 *
 * [themeMode] viene de DataStore, así que el modo claro/oscuro es dinámico de verdad:
 * cambia al instante cuando el usuario lo toca en los ajustes.
 */
@Composable
fun DivTrackTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme
    val extraColors = if (isDarkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalDivTrackColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DivTrackTypography,
            shapes = DivTrackShapes,
            content = content,
        )
    }
}
