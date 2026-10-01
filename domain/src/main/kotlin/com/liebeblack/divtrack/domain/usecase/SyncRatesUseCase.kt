package com.liebeblack.divtrack.domain.usecase

import com.liebeblack.divtrack.core.common.result.Result
import com.liebeblack.divtrack.domain.model.SyncSummary
import com.liebeblack.divtrack.domain.repository.RateRepository
import javax.inject.Inject

/**
 * Fuerza una sincronización contra los proveedores remotos.
 *
 * Nunca lanza: devuelve [Result.Error] tipado. Ese es exactamente el punto donde la UI
 * decide mostrar "Sin conexión. Mostrando última actualización" sin perder lo que ya tiene.
 */
class SyncRatesUseCase @Inject constructor(
    private val repository: RateRepository,
) {
    suspend operator fun invoke(): Result<SyncSummary> = repository.refreshRates()
}
