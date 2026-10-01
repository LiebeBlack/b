package com.liebeblack.divtrack.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Cabeceras comunes. `Accept-Language: es-VE` hace que los proveedores devuelvan nombres de
 * moneda en español cuando los publican.
 */
class HeadersInterceptor(
    private val userAgent: String = "DivTrack-Android",
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header("Accept", "application/json")
            .header("Accept-Language", "es-VE,es;q=0.9,en;q=0.5")
            .header("User-Agent", userAgent)
            .build()

        return chain.proceed(request)
    }
}
