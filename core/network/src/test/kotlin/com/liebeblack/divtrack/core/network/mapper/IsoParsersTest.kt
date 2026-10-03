package com.liebeblack.divtrack.core.network.mapper

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Fechas de los proveedores: lo que se fija aquí es que un día publicado nunca se muestre
 * como el día anterior. La UI pinta en hora de Venezuela, así que una fecha sin hora tiene
 * que interpretarse en esa misma zona (con UTC se adelanta un día).
 */
class IsoParsersTest {

    @Test
    fun `una marca con desplazamiento horario se respeta tal cual`() {
        assertEquals(
            Instant.parse("2026-10-02T04:00:00Z").toEpochMilli(),
            IsoParsers.instantMillisOrNull("2026-10-02T00:00:00-04:00"),
        )
    }

    @Test
    fun `una marca en UTC con milisegundos se respeta`() {
        assertEquals(
            Instant.parse("2026-10-03T04:01:59.305Z").toEpochMilli(),
            IsoParsers.instantMillisOrNull("2026-10-03T04:01:59.305Z"),
        )
    }

    @Test
    fun `una fecha sin hora se lee como medianoche de Venezuela`() {
        assertEquals(
            Instant.parse("2026-09-30T04:00:00Z").toEpochMilli(),
            IsoParsers.instantMillisOrNull("2026-09-30"),
        )
    }

    @Test
    fun `texto vacio o ilegible devuelve nulo en vez de romper la sincronizacion`() {
        assertNull(IsoParsers.instantMillisOrNull(null))
        assertNull(IsoParsers.instantMillisOrNull(""))
        assertNull(IsoParsers.instantMillisOrNull("   "))
        assertNull(IsoParsers.instantMillisOrNull("mañana"))
    }
}
