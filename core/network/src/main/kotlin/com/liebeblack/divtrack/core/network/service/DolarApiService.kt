package com.liebeblack.divtrack.core.network.service

import com.liebeblack.divtrack.core.network.dto.DolarApiHistoryDto
import com.liebeblack.divtrack.core.network.dto.DolarApiRateDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * DolarAPI Venezuela. Los paths son relativos a `BuildConfig.DOLARAPI_BASE_URL`
 * (inyectado desde `local.properties`, sin URLs en el código).
 */
interface DolarApiService {

    @GET("v1/dolares")
    suspend fun getDollars(): List<DolarApiRateDto>

    @GET("v1/dolares/{fuente}")
    suspend fun getDollar(@Path("fuente") fuente: String): DolarApiRateDto

    @GET("v1/historicos/dolares/{fuente}")
    suspend fun getHistory(@Path("fuente") fuente: String): List<DolarApiHistoryDto>
}
