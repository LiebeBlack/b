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
    onBackground = Frost,
    surface = Obsidian,
    onSurface = Frost,
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
    // El tinte M3 tiñe las superficies planas al elevarse: ensucia el look
    // "terminal financiera" de superficies planas con borde. Desactivado.
    surfaceTint = Color.Transparent,
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
    // Ámbar oscuro en terciario: es el color que pinta avisos de dato rancio como TEXTO
    // en modo claro, y el ámbar brillante sobre blanco no llegaba al contraste mínimo.
    tertiary = AmberDeep,
    onTertiary = CloudWhite,
    // Lienzo más profundo que las tarjetas: el modo claro gana la misma separación
    // de capas que siempre tuvo el oscuro (tarjeta blanca que de verdad destaca).
    background = CloudCanvas,
    onBackground = Obsidian,
    surface = CloudSurface,
    onSurface = Obsidian,
    surfaceVariant = CloudShade,
    onSurfaceVariant = SlateText,
    surfaceContainer = CloudSurface,
    surfaceContainerHigh = CloudWhite,
    surfaceContainerLow = CloudShade,
    surfaceContainerLowest = CloudWhite,
    outline = CloudBorder,
    outlineVariant = CloudBorder,
    error = TrendDown,
    onError = CloudWhite,
    surfaceTint = Color.Transparent,
)

private val DarkExtraColors = DivTrackColors(
    trendUp = TrendUp,
    trendDown = TrendDown,
    trendFlat = TrendFlat,
    officialAccent = OfficialAccent,
    parallelAccent = ParallelAccent,
)

private val LightExtraColors = DivTrackColors(
    // Variante *Deep* de cada token semántico: son color de TEXTO (variación %) en claro,
    // y los brillantes no alcanzan el contraste AA sobre blanco (el verde da 2,2:1).
    trendUp = TrendUpDeep,
    trendDown = TrendDownDeep,
    trendFlat = TrendFlatDeep,
    officialAccent = SkyDeep,
    parallelAccent = Amber,
)

// Nota: LightExtraColors mantiene el ámbar brillante en `parallelAccent` porque solo
// usa como FRANJA de color de la tarjeta (fondo sólido), nunca como texto: el texto de
// avisos sale del `tertiary` del esquema, que en modo claro es [AmberDeep].

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
