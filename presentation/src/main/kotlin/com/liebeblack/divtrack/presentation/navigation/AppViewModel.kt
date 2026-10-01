package com.liebeblack.divtrack.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liebeblack.divtrack.domain.model.ThemeMode
import com.liebeblack.divtrack.domain.usecase.EnsureSyncScheduledUseCase
import com.liebeblack.divtrack.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel del shell de la app.
 *
 * Existe por una única razón: el tema tiene que observarse **por encima** de la navegación,
 * porque `DivTrackTheme` envuelve todo el `NavDisplay`. Con `SharingStarted.Eagerly` no hay
 * ni un fotograma pintado con el tema por defecto cuando el usuario ya eligió claro/oscuro.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val ensureSyncScheduled: EnsureSyncScheduledUseCase,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = observeSettings()
        .map { settings -> settings.themeMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.SYSTEM,
        )

    init {
        // El trabajo periódico se alinea con lo que digan las preferencias al arrancar.
        viewModelScope.launch { ensureSyncScheduled() }
    }
}
