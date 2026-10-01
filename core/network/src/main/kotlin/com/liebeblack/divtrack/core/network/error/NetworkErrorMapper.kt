package com.liebeblack.divtrack.core.network.error

import com.liebeblack.divtrack.core.common.error.DataError
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/**
 * Traduce cualquier excepción de la capa de red a la taxonomía [DataError] del núcleo.
 *
 * Es el único punto del proyecto que conoce los tipos de Retrofit/OkHttp/serialización:
 * a partir de aquí, todo el resto del código habla `Result<T>` + [DataError].
 */
@Singleton
class NetworkErrorMapper @Inject constructor() {

    fun map(throwable: Throwable): DataError = when (throwable) {
        is SocketTimeoutException -> DataError.Timeout(cause = throwable)

        is UnknownHostException,
        is ConnectException,
        is NoRouteToHostException,
        -> DataError.Network(cause = throwable)

        is SSLException -> DataError.Network(
            message = "No se pudo establecer una conexión segura",
            cause = throwable,
        )

        is HttpException -> DataError.Http(
            code = throwable.code(),
            message = httpMessage(throwable.code()),
            cause = throwable,
        )

        is SerializationException -> DataError.Parse(cause = throwable)

        is IOException -> DataError.Network(cause = throwable)

        is IllegalArgumentException -> DataError.Parse(cause = throwable)

        else -> DataError.Unknown(cause = throwable)
    }

    private fun httpMessage(code: Int): String = when (code) {
        in 500..599 -> "El proveedor de tasas está fallando (HTTP $code)"
        429 -> "El proveedor limitó las consultas (HTTP 429)"
        404 -> "El proveedor cambió su endpoint (HTTP 404)"
        else -> "Respuesta HTTP $code del proveedor"
    }
}
