package com.liebeblack.divtrack.core.network.logging

import android.util.Log
import com.liebeblack.divtrack.core.common.logging.Logger
import com.liebeblack.divtrack.core.network.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logger de producción. Los niveles `debug`/`warn` se silencian en release para no
 * filtrar información ni gastar ciclos en dispositivos de gama baja; los errores sí se
 * registran siempre.
 */
@Singleton
class LogcatLogger @Inject constructor() : Logger {

    override fun debug(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) Log.w(tag, message, throwable)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}
