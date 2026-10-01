package com.liebeblack.divtrack.domain.usecase

import com.liebeblack.divtrack.domain.model.ProviderDiagnostics
import com.liebeblack.divtrack.domain.repository.RateRepository
import javax.inject.Inject

/**
 * Chequea las fuentes contra sus APIs de verdad y devuelve el diagnóstico.
 *
 * No toca Room ni las tasas guardadas: es una lectura pura de salud de red para la
 * pantalla de Ajustes. Usa el orden de consulta del usuario (o el automático) para que
 * el resultado refleje exactamente lo que haría la próxima sincronización.
 */
class DiagnoseProvidersUseCase @Inject constructor(
    private val repository: RateRepository,
) {
    suspend operator fun invoke(): ProviderDiagnostics = repository.testProviders()
}
