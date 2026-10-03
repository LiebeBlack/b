package com.liebeblack.divtrack.domain.model

/**
 * Resultado de una sincronización de tasas vigentes.
 *
 * @param updatedSources tasas que quedaron escritas en Room.
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
