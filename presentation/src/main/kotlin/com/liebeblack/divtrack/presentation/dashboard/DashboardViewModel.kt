package com.liebeblack.divtrack.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liebeblack.divtrack.core.common.result.Result
import com.liebeblack.divtrack.core.common.utils.CurrencyFormatters
import com.liebeblack.divtrack.domain.model.ExchangeRate
import com.liebeblack.divtrack.domain.model.Spread
import com.liebeblack.divtrack.domain.usecase.CalculateSpreadUseCase
import com.liebeblack.divtrack.domain.usecase.ObserveRatesUseCase
import com.liebeblack.divtrack.domain.usecase.SyncRatesUseCase
import com.liebeblack.divtrack.presentation.R
import com.liebeblack.divtrack.presentation.common.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del dashboard. Agnóstico del framework de UI: no importa nada de Compose
 * (solo `lifecycle-viewmodel`), así que se testea en JVM pura.
 *
 * Flujo Online-First:
 * 1. Al abrir, se suscribe a Room -> la UI muestra lo último guardado al instante.
 * 2. Dispara una sincronización en segundo plano -> si la red responde, Room emite y la
 *    pantalla se repinta sola.
 * 3. Si la red falla, el estado local NO se toca y se emite un efecto para el snackbar
 *    "Sin conexión. Mostrando última actualización".
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeRates: ObserveRatesUseCase,
    private val syncRates: SyncRatesUseCase,
    private val calculateSpread: CalculateSpreadUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<DashboardEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<DashboardEffect> = _effects.asSharedFlow()

    init {
        observeRatesFromCache()
        refresh(isUserInitiated = false)
    }

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.Refresh -> refresh(isUserInitiated = true)
            DashboardIntent.Retry -> refresh(isUserInitiated = true)
        }
    }

    private fun observeRatesFromCache() {
        viewModelScope.launch {
            observeRates().collect { rates -> _state.update { current -> current.withRates(rates) } }
        }
    }

    private fun refresh(isUserInitiated: Boolean) {
        viewModelScope.launch {
            // Solo el refresco manual muestra el indicador: al abrir la app, el esqueleto de
            // carga o los datos ya guardados cuentan la historia y no hace falta un spinner.
            if (isUserInitiated) {
                _state.update { it.copy(isRefreshing = true) }
            }

            when (val result = syncRates()) {
                is Result.Success -> {
                    val summary = result.data
                    _state.update { it.copy(isRefreshing = false, isOffline = false) }

                    when {
                        summary.isPartial -> _effects.emit(
                            DashboardEffect.ShowMessage(UiText.Res(R.string.msg_partial_update)),
                        )

                        isUserInitiated -> _effects.emit(
                            DashboardEffect.ShowMessage(UiText.Res(R.string.msg_rates_updated)),
                        )

                        else -> Unit
                    }
                }

                is Result.Error -> {
                    val hasCachedData = _state.value.hasData
                    _state.update { it.copy(isRefreshing = false, isOffline = true) }

                    // Mensaje exacto del spec cuando hay caché; uno accionable cuando no hay nada.
                    _effects.emit(
                        DashboardEffect.ShowMessage(
                            if (hasCachedData) {
                                UiText.Res(R.string.msg_offline_showing_cache)
                            } else {
                                UiText.Res(R.string.msg_offline_no_data)
                            },
                        ),
                    )
                }

                Result.Loading -> _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /** Aplica las tasas de Room y deriva el spread una sola vez por emisión. */
    private fun DashboardUiState.withRates(rates: List<ExchangeRate>): DashboardUiState {
        val spread: Spread = calculateSpread(rates)
        return copy(
            isLoading = false,
            rates = rates.map { rate -> rate.toRateUiModel() },
            spreadPercentText = spread.percent?.let { percent -> CurrencyFormatters.percent(percent) },
            spreadAbsoluteText = spread.absolute?.let { absolute -> CurrencyFormatters.bolivars(absolute) },
        )
    }
}
