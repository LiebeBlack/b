package com.liebeblack.divtrack.presentation.fake

import com.liebeblack.divtrack.core.common.result.Result
import com.liebeblack.divtrack.domain.model.ExchangeRate
import com.liebeblack.divtrack.domain.model.HistoryRange
import com.liebeblack.divtrack.domain.model.RateHistoryPoint
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.SyncSummary
import com.liebeblack.divtrack.domain.model.ThemeMode
import com.liebeblack.divtrack.domain.model.UserSettings
import com.liebeblack.divtrack.domain.repository.RateRepository
import com.liebeblack.divtrack.domain.repository.SettingsRepository
import com.liebeblack.divtrack.domain.scheduler.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Repositorio de tasas simulado. Permite guionizar la respuesta de la red sin tocar el
 * ViewModel: cada test decide si la sincronización responde, falla o responde a medias.
 */
class FakeRateRepository : RateRepository {

    val rates = MutableStateFlow<List<ExchangeRate>>(emptyList())
    val history = MutableStateFlow<List<RateHistoryPoint>>(emptyList())

    var refreshResult: Result<SyncSummary> = Result.Success(
        SyncSummary(
            updatedSources = emptyList(),
            providerIds = emptyList(),
            failures = emptyList(),
            fetchedAtMillis = 0L,
        ),
    )
    var historyResult: Result<Int> = Result.Success(0)

    var refreshCalls: Int = 0
        private set
    var historySyncCalls: Int = 0
        private set

    override fun observeRates(): Flow<List<ExchangeRate>> = rates

    override fun observeHistory(range: HistoryRange): Flow<List<RateHistoryPoint>> = history

    override suspend fun refreshRates(): Result<SyncSummary> {
        refreshCalls++
        return refreshResult
    }

    override suspend fun syncHistory(force: Boolean): Result<Int> {
        historySyncCalls++
        return historyResult
    }
}

/** Preferencias simuladas en memoria. */
class FakeSettingsRepository(
    initial: UserSettings = UserSettings(),
) : SettingsRepository {

    val settings = MutableStateFlow(initial)

    override fun observeSettings(): Flow<UserSettings> = settings

    override suspend fun setThemeMode(mode: ThemeMode) {
        settings.value = settings.value.copy(themeMode = mode)
    }

    override suspend fun setDefaultSource(source: RateSource) {
        settings.value = settings.value.copy(defaultSource = source)
    }

    override suspend fun setIgtfEnabled(enabled: Boolean) {
        settings.value = settings.value.copy(igtfEnabled = enabled)
    }

    override suspend fun setAutoSyncEnabled(enabled: Boolean) {
        settings.value = settings.value.copy(autoSyncEnabled = enabled)
    }

    override suspend fun setSyncInterval(minutes: Int) {
        settings.value = settings.value.copy(syncIntervalMinutes = minutes)
    }
}

/** Programador simulado: registra lo que WorkManager habría hecho. */
class FakeSyncScheduler : SyncScheduler {

    var scheduledIntervalMinutes: Int? = null
        private set
    var cancelCalls: Int = 0
        private set

    override suspend fun schedulePeriodic(intervalMinutes: Int) {
        scheduledIntervalMinutes = intervalMinutes
    }

    override suspend fun cancelPeriodic() {
        cancelCalls++
    }
}
