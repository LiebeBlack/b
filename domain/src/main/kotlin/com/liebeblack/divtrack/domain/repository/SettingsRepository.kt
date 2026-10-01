package com.liebeblack.divtrack.domain.repository

import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.ThemeMode
import com.liebeblack.divtrack.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** Contrato de preferencias (tema, fuente por defecto, IGTF, sincronización en segundo plano). */
interface SettingsRepository {

    fun observeSettings(): Flow<UserSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDefaultSource(source: RateSource)

    suspend fun setIgtfEnabled(enabled: Boolean)

    suspend fun setAutoSyncEnabled(enabled: Boolean)

    suspend fun setSyncInterval(minutes: Int)
}
