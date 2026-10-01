package com.liebeblack.divtrack.core.network.provider

import com.liebeblack.divtrack.core.network.model.RemoteHistoryPoint
import com.liebeblack.divtrack.core.network.model.RemoteRateSet

/**
 * Contrato de un proveedor de tasas.
 *
 * Se descubren por multibinding de Hilt y se ordenan por [priority] (menor = preferido).
 * Añadir una API nueva es implementar esta interfaz y registrarla en `NetworkModule`:
 * ni el dominio ni la UI cambian.
 */
interface RateProvider {

    /** Identificador estable, se persiste junto a cada tasa para mostrar su procedencia. */
    val id: String

    val priority: Int

    val supportsHistory: Boolean

    /** Lanza excepción si el proveedor falla; el registro aísla el fallo por proveedor. */
    suspend fun fetchLatest(): RemoteRateSet

    /** Histórico diario, o lista vacía si el proveedor no lo ofrece. */
    suspend fun fetchHistory(sourceKey: String): List<RemoteHistoryPoint>
}
