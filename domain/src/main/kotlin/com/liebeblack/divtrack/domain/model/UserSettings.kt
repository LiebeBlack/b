package com.liebeblack.divtrack.domain.model

import com.liebeblack.divtrack.core.common.utils.AppConstants

/** Preferencias del usuario ya interpretadas (enums, no cadenas de DataStore). */
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultSource: RateSource = RateSource.OFICIAL,
    val igtfEnabled: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val syncIntervalMinutes: Int = AppConstants.SYNC_DEFAULT_INTERVAL_MINUTES,
)
