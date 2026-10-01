package com.liebeblack.divtrack.domain.scheduler

/**
 * Programación de la sincronización silenciosa.
 *
 * El dominio declara la intención; `:data` la implementa con WorkManager. Así el dominio
 * sigue siendo Kotlin puro y los tests pueden usar un fake inmediato.
 */
interface SyncScheduler {

    /**
     * Programa (o reprograma) la sincronización periódica. El intervalo mínimo lo impone WorkManager.
     *
     * @param wifiOnly `true` = solo con red no medida (wifi): los datos móviles no se gastan solos.
     */
    suspend fun schedulePeriodic(intervalMinutes: Int, wifiOnly: Boolean)

    suspend fun cancelPeriodic()
}
