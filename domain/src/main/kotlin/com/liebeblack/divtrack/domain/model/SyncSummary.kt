package com.liebeblack.divtrack.domain.model

import com.liebeblack.divtrack.core.common.error.DataError

/** Fallo de un proveedor concreto durante una sincronización. */
data class ProviderFailure(
    val providerId: String,
    val error: DataError,
)

/**
 * Resultado de una sincronización de tasas vigentes.
 *
 * @param failures proveedores que fallaron. Puede haber fallos y aun así [updatedSources]
 * con datos: el multi-proveedor resuelve tasa por tasa, así que la caída de una API no
 * deja la app sin ninguna información.
 */
data class SyncSummary(
    val updatedSources: List<RateSource>,
    val providerIds: List<String>,
    val failures: List<ProviderFailure>,
    val fetchedAtMillis: Long,
) {
    val isPartial: Boolean get() = failures.isNotEmpty() && updatedSources.isNotEmpty()
}
