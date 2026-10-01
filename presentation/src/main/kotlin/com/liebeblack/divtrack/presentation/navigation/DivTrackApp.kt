package com.liebeblack.divtrack.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import com.liebeblack.divtrack.presentation.calculator.CalculatorRoute
import com.liebeblack.divtrack.presentation.dashboard.DashboardRoute
import com.liebeblack.divtrack.presentation.settings.SettingsRoute
import com.liebeblack.divtrack.presentation.theme.DivTrackTheme

/**
 * Punto de entrada de la UI.
 *
 * El tema se aplica aquí, por encima de la navegación, y se alimenta de DataStore: cambiar
 * el modo en Ajustes repinta la app al instante sin reiniciar nada.
 *
 * @param appVersion versión instalada, inyectada desde `:app` (único módulo que conoce
 *   `BuildConfig`), solo para mostrarla en la pantalla de ajustes.
 */
@Composable
fun DivTrackApp(
    appVersion: String = "",
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    DivTrackTheme(themeMode = themeMode) {
        Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DivTrackNavigation(appVersion = appVersion)
        }
    }
}

@Composable
private fun DivTrackNavigation(appVersion: String) {
    // Back stack persistente: sobrevive a la rotación y a la muerte del proceso.
    val backStack = rememberNavBackStack(TopLevelDestination.start.key)
    val currentKey = backStack.lastOrNull() ?: TopLevelDestination.start.key

    // El proveedor de entradas se construye UNA vez. Si se creara en cada recomposición,
    // `NavDisplay` recibiría un grafo distinto en cada pasada y volvería a resolver la
    // entrada activa: la pestaña se reconstruiría (y perdería el scroll) cada vez que algo
    // de arriba cambiara. Es una de las causas clásicas de "la app va lenta".
    val entryProvider = remember(appVersion) {
        entryProvider<NavKey> {
            entry<DashboardKey> { DashboardRoute() }
            entry<CalculatorKey> { CalculatorRoute() }
            entry<SettingsKey> { SettingsRoute(appVersion = appVersion) }
        }
    }

    Scaffold(
        // Cada pantalla trae su propio Scaffold con TopAppBar, que ya aplica el inset de la
        // barra de estado. Aquí se anula para no reservar ese espacio dos veces.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.ordered().forEach { destination ->
                    NavigationBarItem(
                        selected = destination.key == currentKey,
                        onClick = { navigateToTopLevel(backStack, destination) },
                        icon = {
                            Icon(imageVector = destination.icon, contentDescription = null)
                        },
                        label = { Text(text = stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            entryDecorators = listOf(
                // Estado de scroll y de campos guardados por entrada...
                rememberSaveableStateHolderNavEntryDecorator(),
                // ...y un ViewModelStoreOwner propio por entrada: al salir de una pestaña,
                // sus ViewModels se limpian (nada de vistas vivas durante toda la sesión).
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider,
        )
    }
}
