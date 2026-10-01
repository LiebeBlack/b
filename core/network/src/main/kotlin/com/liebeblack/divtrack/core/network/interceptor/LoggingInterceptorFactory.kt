package com.liebeblack.divtrack.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Logging de red SOLO en debug. En release se devuelve `null` y el interceptor ni siquiera
 * se añade a la cadena (ni coste ni posibilidad de filtrar datos de usuario).
 *
 * Nivel HEADERS: suficiente para diagnosticar (URL, códigos, tamaños) sin volcar los
 * cuerpos completos, y sin filtrar a Logcat ninguna tasa con su marca de tiempo.
 */
object LoggingInterceptorFactory {

    fun create(isDebugBuild: Boolean): Interceptor? = if (!isDebugBuild) {
        null
    } else {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            redactHeader("Authorization")
        }
    }
}
