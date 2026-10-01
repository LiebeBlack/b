package com.liebeblack.divtrack.core.common.result

import com.liebeblack.divtrack.core.common.error.DataError
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun `map transforma solo el exito`() {
        val success = Result.Success(2).map { it * 3 }
        assertEquals(6, (success as Result.Success<Int>).data)

        val error: Result<Int> = Result.Error(DataError.Network())
        assertEquals(DataError.Network(), (error.map { it * 3 } as Result.Error).error)
    }

    @Test
    fun `fold cubre los tres casos`() {
        val value = Result.Success(1).fold(
            onSuccess = { it + 1 },
            onError = { -1 },
            onLoading = { -2 },
        )
        assertEquals(2, value)
    }

    @Test
    fun `resultOf convierte la excepcion en error tipado`() = runTest {
        val result: Result<Int> = resultOf(
            mapError = { throwable -> DataError.Network(cause = throwable) },
            block = { throw IOException("sin red") },
        )

        assertTrue(result.isError)
        assertTrue((result as Result.Error).error is DataError.Network)
    }

    @Test
    fun `resultOf no se traga la cancelacion de corrutinas`() = runTest {
        var cancelled = false
        try {
            resultOf(
                mapError = { DataError.Unknown() },
                block = { throw CancellationException("cancelado") },
            )
        } catch (cancellation: CancellationException) {
            cancelled = true
        }
        assertTrue("La cancelación debe propagarse", cancelled)
    }
}
