package com.liebeblack.divtrack.core.network.interceptor

import java.io.IOException
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Degradación elegante a caché HTTP.
 *
 * Si la petición de red falla por conectividad, se intenta responder desde la caché en disco
 * (`FORCE_CACHE`). OkHttp devuelve 504 cuando no hay nada cacheado: en ese caso se propaga el
 * **error original**, que es el que describe lo que pasó de verdad (timeout, DNS, TLS) y el
 * que el usuario verá traducido. Un 504 sintético diría "el proveedor está fallando" cuando
 * el problema era la red del teléfono.
 *
 * La degradación es una mejora, nunca un requisito: si el intento contra la caché también
 * falla, se lanza el error original y la app sigue teniendo Room como fuente de verdad.
 */
class CacheFallbackInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        return try {
            chain.proceed(request)
        } catch (networkFailure: IOException) {
            serveFromCacheOrRethrow(chain, request, networkFailure)
        }
    }

    private fun serveFromCacheOrRethrow(
        chain: Interceptor.Chain,
        request: Request,
        networkFailure: IOException,
    ): Response {
        // Solo las peticiones con cuerpo nulo (GET/HEAD) pueden servirse de caché.
        if (request.body != null) throw networkFailure

        val cachedResponse = try {
            chain.proceed(
                request.newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build(),
            )
        } catch (_: IOException) {
            throw networkFailure
        }

        if (cachedResponse.code == HTTP_UNSATISFIABLE_REQUEST) {
            cachedResponse.close()
            throw networkFailure
        }

        return cachedResponse
    }

    private companion object {
        /** OkHttp usa 504 como "no hay respuesta en caché". */
        const val HTTP_UNSATISFIABLE_REQUEST = 504
    }
}
