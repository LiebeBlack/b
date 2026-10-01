package com.liebeblack.divtrack.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Cabeceras comunes.
 *
 * - `Accept-Language: es-VE` hace que los proveedores devuelvan nombres de moneda en español
 *   cuando los publican.
 * - El `User-Agent` identifica a la app con algo más que una cadena de una palabra: algunos
 *   proveedores están detrás de un CDN que filtra clientes sin `User-Agent` reconocible, y un
 *   `User-Agent` opaco devuelve 403 en lugar de datos. Un 403 se lee en la app como "la fuente
 *   está fallando", así que conviene no provocarlo.
 * - `Cache-Control: no-cache` en las peticiones de tasas: se quiere revalidar con el servidor
 *   (que responde 304 si nada cambió), no servir una copia vieja. Room ya es la caché de la UI.
 */
class HeadersInterceptor(
    private val userAgent: String = USER_AGENT,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header("Accept", "application/json")
            .header("Accept-Language", "es-VE,es;q=0.9,en;q=0.5")
            .header("User-Agent", userAgent)
            .header("Cache-Control", "no-cache")
            .build()

        return chain.proceed(request)
    }

    companion object {
        /** Se mantiene corto y estable: es lo que los proveedores ven en sus métricas. */
        const val USER_AGENT: String = "DivTrack/2.3.7-VT (Android)"
    }
}
