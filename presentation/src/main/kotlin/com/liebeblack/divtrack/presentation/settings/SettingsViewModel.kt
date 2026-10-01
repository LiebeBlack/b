package com.liebeblack.divtrack.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liebeblack.divtrack.domain.usecase.EnsureSyncScheduledUseCase
import com.liebeblack.divtrack.domain.usecase.ObserveSettingsUseCase
import com.liebeblack.divtrack.domain.usecase.UpdateSettingsUseCase
import com.liebeblack.divtrack.presentation.R
import com.liebeblack.divtrack.presentation.common.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Ajustes. No hay estado local duplicado: la pantalla es un reflejo de DataStore, así que
 * cualquier cambio se persiste y vuelve por el mismo flujo (una sola fuente de verdad).
 *
 * Los cambios de sincronización reprograman además el trabajo periódico, para que la
 * preferencia y lo que hace WorkManager nunca se desincronicen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val ensureSyncScheduled: EnsureSyncScheduledUseCase,
) : ViewModel() {

    private val _effects = MutableSharedFlow<SettingsEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<SettingsEffect> = _effects.asSharedFlow()

    val state: StateFlow<SettingsUiState> = observeSettings()
        .map { settings -> settings.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettingsUiState(),
        )

    fun onIntent(intent: SettingsIntent) {
        viewModelScope.launch {
            when (intent) {
                is SettingsIntent.SelectTheme -> updateSettings.setThemeMode(intent.mode)

                is SettingsIntent.SelectDefaultSource ->
                    updateSettings.setDefaultSource(intent.source)

                is SettingsIntent.SetIgtfDefault ->
                    updateSettings.setIgtfEnabled(intent.enabled)

                is SettingsIntent.SetAutoSync -> {
                    updateSettings.setAutoSyncEnabled(intent.enabled)
                    ensureSyncScheduled()
                }

                is SettingsIntent.SetSyncInterval -> {
                    updateSettings.setSyncInterval(intent.minutes)
                    ensureSyncScheduled()
                }
            }

            _effects.emit(SettingsEffect.ShowMessage(UiText.Res(R.string.settings_saved)))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
