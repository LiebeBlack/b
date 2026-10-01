package com.liebeblack.divtrack.presentation.settings

import androidx.compose.runtime.Immutable
import com.liebeblack.divtrack.core.common.utils.AppConstants
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.ThemeMode
import com.liebeblack.divtrack.domain.model.UserSettings
import com.liebeblack.divtrack.presentation.common.UiText

/** Intervalos de sincronización ofrecidos. El mínimo real lo impone WorkManager (15 min). */
val SyncIntervalOptions: List<Int> = listOf(15, 30, 60, 120)

@Immutable
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultSource: RateSource = RateSource.OFICIAL,
    val igtfEnabled: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val syncIntervalMinutes: Int = AppConstants.SYNC_DEFAULT_INTERVAL_MINUTES,
    val isLoading: Boolean = true,
)

/** Dominio -> UI. El estado de la pantalla solo expone lo que se pinta. */
internal fun UserSettings.toUiState(): SettingsUiState = SettingsUiState(
    themeMode = themeMode,
    defaultSource = defaultSource,
    igtfEnabled = igtfEnabled,
    autoSyncEnabled = autoSyncEnabled,
    syncIntervalMinutes = syncIntervalMinutes,
    isLoading = false,
)

sealed interface SettingsIntent {

    data class SelectTheme(val mode: ThemeMode) : SettingsIntent

    data class SelectDefaultSource(val source: RateSource) : SettingsIntent

    data class SetIgtfDefault(val enabled: Boolean) : SettingsIntent

    data class SetAutoSync(val enabled: Boolean) : SettingsIntent

    data class SetSyncInterval(val minutes: Int) : SettingsIntent
}

sealed interface SettingsEffect {

    data class ShowMessage(val text: UiText) : SettingsEffect
}
