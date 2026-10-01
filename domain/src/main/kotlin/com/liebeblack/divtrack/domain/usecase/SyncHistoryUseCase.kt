package com.liebeblack.divtrack.domain.usecase

import com.liebeblack.divtrack.core.common.result.Result
import com.liebeblack.divtrack.domain.repository.RateRepository
import javax.inject.Inject

/**
 * Importa el histórico diario. Aplica un TTL de 24 h para no castigar la red ni la batería:
 * el histórico de días pasados no cambia, así que solo el día en curso necesita refresco.
 */
class SyncHistoryUseCase @Inject constructor(
    private val repository: RateRepository,
) {
    suspend operator fun invoke(force: Boolean = false): Result<Int> = repository.syncHistory(force)
}
