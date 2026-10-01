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

/**
 * Estado de un proveedor tras un chequeo puntual.
 *
 * @param sources claves de tasa que el proveedor respondió (`oficial`, `paralelo`): una
 * conexión "sana" que devuelve JSON sin pares utilizables no cuenta como OK.
 * @param lastUpdatedAtMillis la marca de tiempo MÁS RECIENTE entre lo que trajo: alimenta
 * la edad del dato que se muestra en el diagnóstico. `null` si no publicó ninguna.
 */
data class ProviderStatus(
    val providerId: String,
    val isOk: Boolean,
    val sources: List<String>,
    val lastUpdatedAtMillis: Long? = null,
    val error: ProviderFailure? = null,
)

/**
 * Diagnóstico completo de las fuentes: qué respondió cada una, en su orden de consulta.
 *
 * Alimenta la pantalla de Ajustes; se calcula contra las APIs de verdad en el momento del
 * chequeo, así que refleja el estado real del día y no una opinión cacheada.
 */
data class ProviderDiagnostics(
    val statuses: List<ProviderStatus>,
) {
    val allOk: Boolean get() = statuses.isNotEmpty() && statuses.all { it.isOk }

    val reachableCount: Int get() = statuses.count { it.isOk }

    val total: Int get() = statuses.size
}
