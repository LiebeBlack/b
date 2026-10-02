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
import kotlin.coroutines.cancellation.CancellationException

/**
 * Sincronización silenciosa en segundo plano.
 *
 * No muestra notificaciones ni UI: escribe en Room y, si Room emite, las pantallas se
 * actualizan solas la próxima vez que el usuario las mire. Si la red o el almacenamiento
 * fallan, se devuelve `retry()` y WorkManager aplica su backoff exponencial.
 */
@HiltWorker
class RateSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRates: SyncRatesUseCase,
    private val logger: Logger,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result {
        return try {
            when (val ratesResult = syncRates()) {
                is AppResult.Success -> {
                    logger.debug(TAG, "Tasas sincronizadas: ${ratesResult.data.updatedSources.size} fuentes")
                    ListenableWorker.Result.success()
                }

                is AppResult.Error -> {
                    logger.warn(
                        TAG,
                        "Sincronización falló (intento ${runAttemptCount + 1}): ${ratesResult.error.message}",
                    )
                    ListenableWorker.Result.retry()
                }

                AppResult.Loading -> ListenableWorker.Result.retry()
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            logger.error(
                TAG,
                "Error inesperado en sincronización (intento ${runAttemptCount + 1}): ${exception.message}",
                exception,
            )
            ListenableWorker.Result.retry()
        }
    }

    companion object {
        private const val TAG = "RateSyncWorker"
    }
}
