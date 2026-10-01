package com.liebeblack.divtrack.core.network.service

import com.liebeblack.divtrack.core.network.dto.DolarApiRateDto
import retrofit2.http.GET

/**
 * DolarAPI Venezuela. Los paths son relativos a `BuildConfig.DOLARAPI_BASE_URL`
 * (inyectado desde `local.properties`, sin URLs en el código).
 */
interface DolarApiService {

    /**
     * Devuelve todas las tasas publicadas (oficial + paralelo) en una sola llamada.
     *
     * Es el único endpoint de la app: el histórico por fuente se eliminó con la pantalla
     * de Histórico, así que aquí no queda ninguna llamada que devuelva series diarias.
     */
    @GET("v1/dolares")
    suspend fun getDollars(): List<DolarApiRateDto>
}
