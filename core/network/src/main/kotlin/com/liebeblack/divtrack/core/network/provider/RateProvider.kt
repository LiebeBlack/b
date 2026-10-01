package com.liebeblack.divtrack.core.network.provider

import com.liebeblack.divtrack.core.network.model.RemoteRateSet

/**
 * Contrato de un proveedor de tasas.
 *
 * Se descubren por multibinding de Hilt y se ordenan por [priority] (menor = preferido).
 * Añadir una API nueva es implementar esta interfaz y registrarla en `NetworkModule`:
 * ni el dominio ni la UI cambian.
 *
 * El contrato es intencionadamente mínimo: una sola llamada que devuelve las tasas
 * vigentes. Cualquier método extra (series históricas, por ejemplo) obliga a implementarlo
 * en todos los proveedores aunque solo uno lo use, y acaba siendo código muerto.
 */
interface RateProvider {

    /** Identificador estable, se persiste junto a cada tasa para mostrar su procedencia. */
    val id: String

    val priority: Int

    /** Lanza excepción si el proveedor falla; el registro aísla el fallo por proveedor. */
    suspend fun fetchLatest(): RemoteRateSet
}
