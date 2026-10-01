package com.liebeblack.divtrack

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application de DivTrack.
 *
 * Implementa [Configuration.Provider] para que WorkManager use la fábrica de Hilt: así el
 * `RateSyncWorker` recibe sus dependencias por inyección en lugar de construirlas a mano
 * (que es de donde salen los `ServiceLocator` y los acoplamientos que Clean Architecture
 * intenta evitar).
 */
@HiltAndroidApp
class DivTrackApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()
}
