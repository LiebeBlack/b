package com.liebeblack.divtrack.core.network.service

import com.liebeblack.divtrack.core.network.dto.YadioRatesDto
import retrofit2.http.GET

/** Yadio (proveedor de respaldo del paralelo). Base en `BuildConfig.YADIO_BASE_URL`. */
interface YadioService {

    @GET("exrates/USD")
    suspend fun getRates(): YadioRatesDto
}
