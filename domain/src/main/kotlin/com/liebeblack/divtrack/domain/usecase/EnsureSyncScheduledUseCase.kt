package com.liebeblack.divtrack.domain.usecase

import com.liebeblack.divtrack.domain.repository.SettingsRepository
import com.liebeblack.divtrack.domain.scheduler.SyncScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Alinea el trabajo periódico con las preferencias del usuario.
 *
 * Se invoca al arrancar la app y cada vez que cambian la auto-sincronización, el intervalo
 * o el ajuste de solo-wifi: una sola fuente de verdad (DataStore) decide si WorkManager
 * trabaja, con qué frecuencia y con qué restricción de red.
 */
@Singleton
class EnsureSyncScheduledUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncScheduler: SyncScheduler,
) {
    private val mutex = Mutex()

    suspend operator fun invoke() = mutex.withLock {
        val settings = settingsRepository.observeSettings().first()
        if (settings.autoSyncEnabled) {
            syncScheduler.schedulePeriodic(settings.syncIntervalMinutes, settings.syncOnWifiOnly)
        } else {
            syncScheduler.cancelPeriodic()
        }
    }
}
