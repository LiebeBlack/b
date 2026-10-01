package com.liebeblack.divtrack.data.mapper

import com.liebeblack.divtrack.core.datastore.model.UserPreferences
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.ThemeMode
import com.liebeblack.divtrack.domain.model.UserSettings

/**
 * DataStore (primitivos) -> dominio (enums). Este mapeo es la razón de que
 * `:core:datastore` no necesite conocer el dominio.
 */
internal fun UserPreferences.toDomain(): UserSettings = UserSettings(
    themeMode = ThemeMode.fromStorage(themeMode),
    defaultSource = RateSource.fromKey(defaultSourceKey) ?: RateSource.OFICIAL,
    igtfEnabled = igtfEnabled,
    autoSyncEnabled = autoSyncEnabled,
    syncIntervalMinutes = syncIntervalMinutes,
)
