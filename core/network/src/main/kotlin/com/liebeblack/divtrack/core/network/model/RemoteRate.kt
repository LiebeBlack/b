package com.liebeblack.divtrack.core.network.model

/**
 * Tasa tal como la entrega un proveedor remoto. Vive en `:core:network` y NO conoce el
 * dominio: el mapeo a modelos de negocio ocurre en `:data` (dirección de dependencias).
 */
data class RemoteRate(
    /** Clave canónica de la tasa (`oficial`, `paralelo`). */
    val sourceKey: String,
    val value: Double,
    /** Marca de tiempo del proveedor, si la publica. */
    val updatedAtMillis: Long?,
)

/** Conjunto de tasas devuelto por un proveedor en una pasada. */
data class RemoteRateSet(
    val providerId: String,
    val rates: List<RemoteRate>,
    val fetchedAtMillis: Long,
)

/** Punto de histórico ya normalizado a día (epochDay) para Room. */
data class RemoteHistoryPoint(
    val sourceKey: String,
    val epochDay: Long,
    val value: Double,
)
