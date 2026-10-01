package com.liebeblack.divtrack.domain.usecase

import com.liebeblack.divtrack.domain.model.ExchangeRate
import com.liebeblack.divtrack.domain.repository.RateRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Observa las tasas vigentes. Emite desde Room, así que el dashboard se repinta solo
 * cuando la red escribe y sigue mostrando datos sin conexión.
 */
class ObserveRatesUseCase @Inject constructor(
    private val repository: RateRepository,
) {
    operator fun invoke(): Flow<List<ExchangeRate>> = repository.observeRates()
}
