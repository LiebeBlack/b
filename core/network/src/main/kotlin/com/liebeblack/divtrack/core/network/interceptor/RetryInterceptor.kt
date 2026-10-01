package com.liebeblack.divtrack.core.network.interceptor

import com.liebeblack.divtrack.core.common.utils.AppConstants
import java.io.IOException
import kotlin.random.Random
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Reintentos automáticos con backoff exponencial + jitter.
 *
 * - Reintenta ante fallos transitorios: `IOException`, 408, 429 y 5xx.
 * - Respeta `Retry-After` del servidor, acotado para no dejar al usuario esperando.
 * - Cierra la respuesta antes de reintentar (si no, OkHttp filtra conexiones).
 *
 * Es un interceptor de aplicación: puede dormir sin bloquear el hilo principal porque
 * OkHttp ejecuta la cadena en sus propios hilos de dispatcher.
 */
class RetryInterceptor(
    private val maxRetries: Int = AppConstants.HTTP_MAX_RETRIES,
    private val baseDelayMillis: Long = 350L,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0

        while (true) {
            try {
                val response = chain.proceed(request)

                if (attempt >= maxRetries || !response.isRetryable()) return response

                val retryAfterMillis = response.retryAfterMillis()
                response.close()
                attempt++
                sleep(attempt, retryAfterMillis)
            } catch (io: IOException) {
                if (attempt >= maxRetries) throw io
                attempt++
                sleep(attempt, retryAfterMillis = null)
            }
        }
    }

    private fun Response.isRetryable(): Boolean =
        code == HTTP_REQUEST_TIMEOUT ||
            code == HTTP_TOO_MANY_REQUESTS ||
            code in HTTP_SERVER_ERROR_RANGE

    private fun Response.retryAfterMillis(): Long? =
        header("Retry-After")?.trim()?.toLongOrNull()?.times(SECONDS_TO_MILLIS)

    private fun sleep(attempt: Int, retryAfterMillis: Long?) {
        val delay = retryAfterMillis
            ?.coerceAtMost(MAX_RETRY_AFTER_MILLIS)
            ?: exponentialWithJitter(attempt)
        Thread.sleep(delay)
    }

    private fun exponentialWithJitter(attempt: Int): Long {
        val exponential = baseDelayMillis * (1L shl (attempt - 1).coerceAtLeast(0))
        val jitter = Random.nextLong(0L, baseDelayMillis.coerceAtLeast(1L))
        return (exponential + jitter).coerceAtMost(MAX_BACKOFF_MILLIS)
    }

    private companion object {
        const val HTTP_REQUEST_TIMEOUT = 408
        const val HTTP_TOO_MANY_REQUESTS = 429
        val HTTP_SERVER_ERROR_RANGE = 500..599
        const val SECONDS_TO_MILLIS = 1_000L
        const val MAX_BACKOFF_MILLIS = 2_000L
        const val MAX_RETRY_AFTER_MILLIS = 3_000L
    }
}
