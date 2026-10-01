package com.liebeblack.divtrack.core.network.interceptor

import java.io.IOException
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Degradación elegante a caché HTTP.
 *
 * Si la petición de red falla por un problema de conectividad, se reintenta internamente
 * contra la caché en disco (`FORCE_CACHE`). OkHttp devuelve 504 cuando no hay nada
 * cacheado: en ese caso se propaga el error original para que el repositorio lo traduzca
 * y la UI muestre el aviso de "sin conexión" con los datos de Room.
 */
class CacheFallbackInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        return try {
            chain.proceed(request)
        } catch (io: IOException) {
            val cachedRequest = request.newBuilder()
                .cacheControl(CacheControl.FORCE_CACHE)
                .build()

            val cachedResponse = chain.proceed(cachedRequest)
            if (cachedResponse.code == HTTP_UNSATISFIABLE_REQUEST) {
                cachedResponse.close()
                throw io
            }
            cachedResponse
        }
    }

    private companion object {
        /** OkHttp usa 504 como "no hay respuesta en caché". */
        const val HTTP_UNSATISFIABLE_REQUEST = 504
    }
}
