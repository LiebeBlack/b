package com.liebeblack.divtrack.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.liebeblack.divtrack.core.common.utils.AppConstants
import com.liebeblack.divtrack.core.common.utils.SourceKeys
import com.liebeblack.divtrack.core.datastore.model.UserPreferences
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Contrato de preferencias. Es una interfaz para poder testear el repositorio en JVM pura
 * (la implementación real depende de DataStore/Android).
 */
interface UserPreferencesDataSource {

    val preferences: Flow<UserPreferences>

    suspend fun setThemeMode(themeMode: String)

    suspend fun setDefaultSource(sourceKey: String)

    suspend fun setIgtfEnabled(enabled: Boolean)

    suspend fun setAutoSyncEnabled(enabled: Boolean)

    suspend fun setSyncIntervalMinutes(minutes: Int)

    suspend fun setLastSyncAt(millis: Long)
}

@Singleton
class DataStoreUserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesDataSource {

    override val preferences: Flow<UserPreferences> = dataStore.data
        // Un archivo corrupto o ilegible no debe tumbar la app: se parte de valores vacíos
        // (los defaults del modelo) y se reescribe en el siguiente cambio del usuario.
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> preferences.toUserPreferences() }

    override suspend fun setThemeMode(themeMode: String) {
        dataStore.edit { it[Keys.THEME_MODE] = normaliseThemeMode(themeMode) }
    }

    override suspend fun setDefaultSource(sourceKey: String) {
        val normalised = sourceKey.lowercase().takeIf { it in SourceKeys.all } ?: SourceKeys.OFICIAL
        dataStore.edit { it[Keys.DEFAULT_SOURCE] = normalised }
    }

    override suspend fun setIgtfEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.IGTF_ENABLED] = enabled }
    }

    override suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_SYNC] = enabled }
    }

    override suspend fun setSyncIntervalMinutes(minutes: Int) {
        dataStore.edit {
            it[Keys.SYNC_INTERVAL] = minutes.coerceAtLeast(AppConstants.SYNC_MIN_INTERVAL_MINUTES)
        }
    }

    override suspend fun setLastSyncAt(millis: Long) {
        dataStore.edit { it[Keys.LAST_SYNC_AT] = millis }
    }

    private fun normaliseThemeMode(value: String): String = when (value.uppercase()) {
        UserPreferences.THEME_LIGHT -> UserPreferences.THEME_LIGHT
        UserPreferences.THEME_DARK -> UserPreferences.THEME_DARK
        else -> UserPreferences.THEME_SYSTEM
    }

    private fun Preferences.toUserPreferences(): UserPreferences = UserPreferences(
        themeMode = this[Keys.THEME_MODE] ?: UserPreferences.THEME_SYSTEM,
        defaultSourceKey = this[Keys.DEFAULT_SOURCE] ?: SourceKeys.OFICIAL,
        igtfEnabled = this[Keys.IGTF_ENABLED] ?: false,
        autoSyncEnabled = this[Keys.AUTO_SYNC] ?: true,
        syncIntervalMinutes = this[Keys.SYNC_INTERVAL]
            ?: AppConstants.SYNC_DEFAULT_INTERVAL_MINUTES,
        lastSyncAtMillis = this[Keys.LAST_SYNC_AT],
    )

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_SOURCE = stringPreferencesKey("default_source")
        val IGTF_ENABLED = booleanPreferencesKey("igtf_enabled")
        val AUTO_SYNC = booleanPreferencesKey("auto_sync_enabled")
        val SYNC_INTERVAL = intPreferencesKey("sync_interval_minutes")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }
}
