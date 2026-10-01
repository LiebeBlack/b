package com.liebeblack.divtrack.presentation.fake

import com.liebeblack.divtrack.core.common.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Reloj fijo para los tests de presentación (misma idea que el fake de `:data`). */
class FakeTimeProvider(
    var nowMillis: Long = 1_800_000_000_000L,
) : TimeProvider {

    override fun nowMillis(): Long = nowMillis

    override fun nowInstant(): Instant = Instant.ofEpochMilli(nowMillis)

    override fun today(): LocalDate = LocalDate.ofInstant(nowInstant(), zone())

    override fun zone(): ZoneId = ZoneId.of("America/Caracas")
}
