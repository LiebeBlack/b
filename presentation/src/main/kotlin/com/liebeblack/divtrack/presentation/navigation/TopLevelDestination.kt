package com.liebeblack.divtrack.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.liebeblack.divtrack.presentation.R
import kotlinx.serialization.Serializable

/**
 * Claves de navegación de Navigation 3.
 *
 * Son `data object` serializables (no cadenas): `rememberNavBackStack` las guarda en el
 * estado de la Activity, así que sobreviven a la rotación y a la reconstrucción por falta
 * de memoria. Al ser objetos únicos, `equals` es trivial y el `NavDisplay` no se confunde.
 */
@Serializable
data object DashboardKey : NavKey

@Serializable
data object CalculatorKey : NavKey

@Serializable
data object SettingsKey : NavKey

/**
 * Pestañas de la barra inferior.
 *
 * La app tiene exactamente tres pantallas planas (sin jerarquía): modelo el back stack
 * como `[raíz, pestaña actual]`, de modo que "atrás" vuelve al panel de tasas y, desde ahí,
 * sale de la app. Es el comportamiento que Android espera de una barra de navegación.
 *
 * `ordered` devuelve una lista ya construida (una sola vez, en la carga de la clase): antes
 * se recalculaba en cada recomposición de la barra inferior.
 */
enum class TopLevelDestination(
    val key: NavKey,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD(DashboardKey, R.string.nav_dashboard, Icons.AutoMirrored.Filled.TrendingUp),
    CALCULATOR(CalculatorKey, R.string.nav_calculator, Icons.Filled.Calculate),
    SETTINGS(SettingsKey, R.string.nav_settings, Icons.Filled.Settings);

    companion object {
        val start: TopLevelDestination = DASHBOARD

        private val orderedDestinations: List<TopLevelDestination> = entries.toList()

        fun ordered(): List<TopLevelDestination> = orderedDestinations
    }
}

/**
 * Cambia de pestaña manteniendo el back stack acotado a dos entradas.
 *
 * Se limpian las entradas intermedias antes de añadir la nueva: sin esto, alternar entre
 * pestañas haría crecer la pila sin límite y el botón "atrás" tendría que deshacer un
 * recorrido que el usuario no percibe.
 */
fun navigateToTopLevel(backStack: MutableList<NavKey>, destination: TopLevelDestination) {
    if (backStack.lastOrNull() == destination.key) return

    val root = TopLevelDestination.start.key
    backStack.removeAll { it != root }
    if (backStack.isEmpty()) backStack.add(root)
    if (destination.key != root) backStack.add(destination.key)
}
