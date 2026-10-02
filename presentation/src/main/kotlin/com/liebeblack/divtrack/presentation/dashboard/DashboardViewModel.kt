package com.liebeblack.divtrack.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liebeblack.divtrack.core.common.error.DataError
import com.liebeblack.divtrack.core.common.result.Result
import com.liebeblack.divtrack.core.common.utils.CurrencyFormatters
import com.liebeblack.divtrack.core.common.time.TimeProvider
import com.liebeblack.divtrack.domain.model.ExchangeRate
import com.liebeblack.divtrack.domain.model.Spread
import com.liebeblack.divtrack.domain.usecase.CalculateSpreadUseCase
import com.liebeblack.divtrack.domain.usecase.ObserveRatesUseCase
import com.liebeblack.divtrack.domain.usecase.SyncRatesUseCase
import com.liebeblack.divtrack.presentation.R
import com.liebeblack.divtrack.presentation.common.UiText
import com.liebeblack.divtrack.presentation.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
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
 * Al abrir, sincroniza primero y mantiene el estado de carga visible para que las tasas
 * guardadas no aparezcan un instante antes de ser reemplazadas. Al terminar, Room entrega
 * el dato actualizado o conserva el último valor si la red falló.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeRates: ObserveRatesUseCase,
    private val syncRates: SyncRatesUseCase,
    private val calculateSpread: CalculateSpreadUseCase,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<DashboardEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<DashboardEffect> = _effects.asSharedFlow()

    private var refreshJob: Job? = null
    private var refreshRequestedByUser = false
    private var ratesObserved = false

    init {
        val initialRefresh = refresh(isUserInitiated = false)
        viewModelScope.launch {
            initialRefresh.join()
            observeRatesFromCache()
        }
    }

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.Refresh -> refresh(isUserInitiated = true)
            DashboardIntent.Retry -> refresh(isUserInitiated = true)
        }
    }

    private fun observeRatesFromCache() {
        viewModelScope.launch {
            observeRates().collect { rates ->
                ratesObserved = true
                _state.update { current -> current.withRates(rates) }
            }
        }
    }

    private fun refresh(isUserInitiated: Boolean): Job {
        val activeRefresh = refreshJob
        if (activeRefresh?.isActive == true) {
            if (isUserInitiated) {
                refreshRequestedByUser = true
                _state.update { it.copy(isRefreshing = true) }
            }
            return activeRefresh
        }

        refreshRequestedByUser = isUserInitiated
        return viewModelScope.launch {
            if (isUserInitiated) {
                _state.update { it.copy(isRefreshing = true) }
            }

            try {
                when (val result = syncRates()) {
                    is Result.Success -> {
                        val summary = result.data
                        _state.update {
                            it.copy(
                                isRefreshing = false,
                                isOffline = false,
                                isConnectivityProblem = false,
                                errorText = null,
                            )
                        }

                        when {
                            summary.isPartial -> _effects.emit(
                                DashboardEffect.ShowMessage(UiText.Res(R.string.msg_partial_update)),
                            )

                            refreshRequestedByUser -> _effects.emit(
                                DashboardEffect.ShowMessage(UiText.Res(R.string.msg_rates_updated)),
                            )

                            else -> Unit
                        }
                    }

                    is Result.Error -> {
                        val error = result.error
                        val hasCachedData = _state.value.hasData
                        val isConnectivity = error is DataError.Network

                        _state.update {
                            it.copy(
                                isRefreshing = false,
                                isOffline = true,
                                isConnectivityProblem = isConnectivity,
                                errorText = error.toUiText(),
                            )
                        }

                        // No todos los fallos son "sin conexión": si el proveedor responde 503 o
                        // la petición caduca, el usuario tiene que leer eso y no un diagnóstico
                        // equivocado de su propia red. Cuando sí es conectividad, el mensaje
                        // recuerda que los datos en pantalla siguen siendo válidos.
                        if (refreshRequestedByUser && ratesObserved) {
                            _effects.emit(
                                DashboardEffect.ShowMessage(
                                    when {
                                        isConnectivity && hasCachedData ->
                                            UiText.Res(R.string.msg_offline_showing_cache)

                                        isConnectivity -> UiText.Res(R.string.msg_offline_no_data)

                                        else -> error.toUiText()
                                    },
                                ),
                            )
                        }
                    }

                    Result.Loading -> Unit
                }
            } finally {
                refreshRequestedByUser = false
                _state.update { it.copy(isLoading = !ratesObserved, isRefreshing = false) }
            }
        }.also { refreshJob = it }
    }

    /** Aplica las tasas de Room y deriva el spread una sola vez por emisión. */
    private fun DashboardUiState.withRates(rates: List<ExchangeRate>): DashboardUiState {
        val spread: Spread = calculateSpread(rates)
        return copy(
            isLoading = false,
            rates = rates.map { rate -> rate.toRateUiModel(timeProvider.nowMillis()) },
            spreadPercentText = spread.percent?.let { percent -> CurrencyFormatters.percent(percent) },
            spreadAbsoluteText = spread.absolute?.let { absolute -> CurrencyFormatters.bolivars(absolute) },
        )
    }
}
