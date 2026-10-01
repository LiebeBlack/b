package com.liebeblack.divtrack.data.scheduler

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.liebeblack.divtrack.core.common.utils.AppConstants
import com.liebeblack.divtrack.data.worker.RateSyncWorker
import com.liebeblack.divtrack.domain.scheduler.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación con WorkManager.
 *
 * Detalles que importan en gama baja:
 * - Restricción de red obligatoria: nunca despierta el proceso para fallar por falta de datos.
 * - Trabajo periódico con `ExistingPeriodicWorkPolicy.UPDATE`: cambiar el intervalo no borra
 *   ni duplica la programación existente.
 * - Backoff exponencial: si el proveedor está caído, no se martillea la API.
 */
@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncScheduler {

    override suspend fun schedulePeriodic(intervalMinutes: Int, wifiOnly: Boolean) {
        val interval = intervalMinutes
            .coerceAtLeast(AppConstants.SYNC_MIN_INTERVAL_MINUTES)
            .toLong()

        val request = PeriodicWorkRequestBuilder<RateSyncWorker>(interval, TimeUnit.MINUTES)
            .setConstraints(defaultConstraints(wifiOnly))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override suspend fun cancelPeriodic() {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    /**
     * `CONNECTED` por defecto (cualquier red). Con `wifiOnly`, `UNMETERED`: wifi o red
     * equivalente sin límite de datos — los datos móviles del usuario no se gastan solos.
     */
    private fun defaultConstraints(wifiOnly: Boolean): Constraints = Constraints.Builder()
        .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
        .build()

    private companion object {
        const val PERIODIC_WORK_NAME = "divtrack_rate_sync_periodic"
        const val BACKOFF_SECONDS = 30L
    }
}
