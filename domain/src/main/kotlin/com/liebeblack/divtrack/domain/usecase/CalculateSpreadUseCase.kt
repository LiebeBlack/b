package com.liebeblack.divtrack.domain.usecase

import com.liebeblack.divtrack.domain.model.ExchangeRate
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.Spread
import javax.inject.Inject

/** Diferencia porcentual y absoluta entre paralelo y oficial. */
class CalculateSpreadUseCase @Inject constructor() {

    operator fun invoke(rates: List<ExchangeRate>): Spread = Spread.calculate(
        oficial = rates.firstOrNull { it.source == RateSource.OFICIAL }?.value,
        paralelo = rates.firstOrNull { it.source == RateSource.PARALELO }?.value,
    )
}
