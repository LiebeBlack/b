package com.liebeblack.divtrack.core.network.interceptor

import java.io.IOException
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Degradación elegante a caché HTTP.
 *
 * Si la petición falla por conectividad o recibe un error HTTP temporal, se intenta responder
 * desde la caché en disco (`FORCE_CACHE`). OkHttp devuelve 504 cuando no hay nada cacheado:
 * en ese caso se propaga el error original, que es el que describe lo que pasó de verdad.
 *
 * La degradación es una mejora, nunca un requisito: si el intento contra la caché también
 * falla, se lanza el error original y la app sigue teniendo Room como fuente de verdad.
 */
class CacheFallbackInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val response = try {
            chain.proceed(request)
        } catch (networkFailure: IOException) {
            return serveFromCacheOrRethrow(chain, request, networkFailure)
        }

        if (!response.isTemporaryFailure()) return response

        val cachedResponse = request.fromCacheOnly(chain) ?: return response
        response.close()
        return cachedResponse
    }

    private fun serveFromCacheOrRethrow(
        chain: Interceptor.Chain,
        request: Request,
        networkFailure: IOException,
    ): Response {
        // Solo las peticiones con cuerpo nulo (GET/HEAD) pueden servirse de caché.
        if (request.body != null) throw networkFailure

        return request.fromCacheOnly(chain)
            ?: throw networkFailure
    }

    private fun Request.fromCacheOnly(chain: Interceptor.Chain): Response? {
        if (body != null) return null

        val cachedResponse = try {
            chain.proceed(newBuilder().cacheControl(CacheControl.FORCE_CACHE).build())
        } catch (_: IOException) {
            return null
        }

        if (cachedResponse.code == HTTP_UNSATISFIABLE_REQUEST) {
            cachedResponse.close()
            return null
        }

        return cachedResponse
    }

    private fun Response.isTemporaryFailure(): Boolean =
        code == HTTP_REQUEST_TIMEOUT ||
            code == HTTP_TOO_MANY_REQUESTS ||
            code in HTTP_SERVER_ERROR_RANGE

    private companion object {
        /** OkHttp usa 504 como "no hay respuesta en caché". */
        const val HTTP_UNSATISFIABLE_REQUEST = 504
        const val HTTP_REQUEST_TIMEOUT = 408
        const val HTTP_TOO_MANY_REQUESTS = 429
        val HTTP_SERVER_ERROR_RANGE = 500..599
    }
}
