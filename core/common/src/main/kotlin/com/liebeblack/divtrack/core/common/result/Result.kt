package com.liebeblack.divtrack.core.common.result

import com.liebeblack.divtrack.core.common.error.DataError

/**
 * Resultado de una operación de datos.
 *
 * - [Success] y [Error] son lo que devuelven los contratos `suspend`.
 * - [Loading] lo emiten únicamente los flujos de refresco (nunca una función `suspend`),
 *   porque "cargando" es un estado de presentación y no un resultado.
 *
 * El repositorio es la frontera donde se atrapan las excepciones de red
 * (`IOException`, `SerializationException`, `HttpException`, ...): hacia arriba nunca
 * sube una excepción, siempre un [Error] tipado con el que la UI puede decidir.
 */
sealed interface Result<out T> {

    data class Success<out T>(val data: T) : Result<T>

    data class Error(val error: DataError) : Result<Nothing>

    data object Loading : Result<Nothing>
}

/** Transforma el valor de éxito conservando el error. */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    Result.Loading -> Result.Loading
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (DataError) -> Unit): Result<T> {
    if (this is Result.Error) action(error)
    return this
}

inline fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onError: (DataError) -> R,
    onLoading: () -> R,
): R = when (this) {
    is Result.Success -> onSuccess(data)
    is Result.Error -> onError(error)
    Result.Loading -> onLoading()
}

val <T> Result<T>.dataOrNull: T? get() = (this as? Result.Success)?.data

val Result<*>.errorOrNull: DataError? get() = (this as? Result.Error)?.error

val Result<*>.isSuccess: Boolean get() = this is Result.Success

val Result<*>.isError: Boolean get() = this is Result.Error

/**
 * Ejecuta [block] y convierte cualquier excepción recuperable en [Result.Error] usando
 * [mapError]. Los errores fatales de la JVM se dejan propagar.
 *
 * Re-lanza [kotlin.coroutines.cancellation.CancellationException] para no romper la
 * cancelación estructurada de corrutinas.
 */
suspend fun <T> resultOf(
    mapError: (Throwable) -> DataError,
    block: suspend () -> T,
): Result<T> = try {
    Result.Success(block())
} catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
    throw cancellation
} catch (exception: Exception) {
    Result.Error(mapError(exception))
}
