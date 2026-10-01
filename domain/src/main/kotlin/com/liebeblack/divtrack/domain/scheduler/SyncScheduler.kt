package com.liebeblack.divtrack.domain.scheduler

/**
 * Programación de la sincronización silenciosa.
 *
 * El dominio declara la intención; `:data` la implementa con WorkManager. Así el dominio
 * sigue siendo Kotlin puro y los tests pueden usar un fake inmediato.
 */
interface SyncScheduler {

    /** Programa (o reprograma) la sincronización periódica. El intervalo mínimo lo impone WorkManager. */
    suspend fun schedulePeriodic(intervalMinutes: Int)

    suspend fun cancelPeriodic()
}
