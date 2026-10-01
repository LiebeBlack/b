package com.liebeblack.divtrack.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.liebeblack.divtrack.core.common.logging.Logger
import com.liebeblack.divtrack.core.common.result.Result as AppResult
import com.liebeblack.divtrack.domain.usecase.SyncRatesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Sincronización silenciosa en segundo plano.
 *
 * No muestra notificaciones ni UI: escribe en Room y, si Room emite, las pantallas se
 * actualizan solas la próxima vez que el usuario las mire. Si la red falla, se devuelve
 * `retry()` con el backoff configurado por el scheduler.
 */
@HiltWorker
class RateSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRates: SyncRatesUseCase,
    private val logger: Logger,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result {
        return when (val ratesResult = syncRates()) {
            is AppResult.Success -> {
                logger.debug(TAG, "Tasas sincronizadas: ${ratesResult.data.updatedSources.size} fuentes")
                ListenableWorker.Result.success()
            }

            is AppResult.Error -> {
                val isLastAttempt = runAttemptCount >= MAX_ATTEMPTS - 1
                val message = "Sincronización falló (intento ${runAttemptCount + 1}): ${ratesResult.error.message}"

                // Los reintentos intermedios son ruido; el fallo definitivo es un error real.
                if (isLastAttempt) {
                    logger.error(TAG, message)
                    ListenableWorker.Result.failure()
                } else {
                    logger.warn(TAG, message)
                    ListenableWorker.Result.retry()
                }
            }

            AppResult.Loading -> ListenableWorker.Result.retry()
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val TAG = "RateSyncWorker"
    }
}
