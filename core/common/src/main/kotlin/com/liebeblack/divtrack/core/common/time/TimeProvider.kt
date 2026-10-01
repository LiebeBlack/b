package com.liebeblack.divtrack.core.common.time

import com.liebeblack.divtrack.core.common.utils.AppConstants
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Fuente de tiempo inyectable. Existe para dos motivos concretos:
 *
 * 1. El "cierre anterior" y el corte del día se calculan en hora de Venezuela, no en la zona
 *    del dispositivo (un usuario de viaje no debe ver un día distinto).
 * 2. Los tests fijan el reloj y dejan de depender del día en que se ejecuten.
 */
interface TimeProvider {
    fun nowMillis(): Long
    fun nowInstant(): Instant

    /** Día de calendario en la zona de Venezuela. */
    fun today(): LocalDate

    fun zone(): ZoneId
}

/** Implementación de producción. Se enlaza por Hilt en el módulo `:data`. */
class SystemTimeProvider(
    private val zoneId: ZoneId = ZoneId.of(AppConstants.TIME_ZONE_ID),
) : TimeProvider {

    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun nowInstant(): Instant = Instant.ofEpochMilli(nowMillis())

    override fun today(): LocalDate = LocalDate.now(zoneId)

    override fun zone(): ZoneId = zoneId
}
