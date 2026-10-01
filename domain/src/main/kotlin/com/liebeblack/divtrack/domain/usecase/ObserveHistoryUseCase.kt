package com.liebeblack.divtrack.domain.usecase

import com.liebeblack.divtrack.domain.model.HistoryRange
import com.liebeblack.divtrack.domain.model.RateHistoryPoint
import com.liebeblack.divtrack.domain.repository.RateRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Observa la serie diaria dentro del rango pedido (1M / 3M / YTD / 1A). */
class ObserveHistoryUseCase @Inject constructor(
    private val repository: RateRepository,
) {
    operator fun invoke(range: HistoryRange): Flow<List<RateHistoryPoint>> =
        repository.observeHistory(range)
}
