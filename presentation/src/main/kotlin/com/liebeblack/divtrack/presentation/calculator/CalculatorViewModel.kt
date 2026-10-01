package com.liebeblack.divtrack.presentation.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liebeblack.divtrack.core.common.utils.AppConstants
import com.liebeblack.divtrack.core.common.utils.CurrencyFormatters
import com.liebeblack.divtrack.core.common.utils.NumberParsing
import com.liebeblack.divtrack.domain.model.Conversion
import com.liebeblack.divtrack.domain.model.ConversionDirection
import com.liebeblack.divtrack.domain.model.ExchangeRate
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.usecase.CalculateConversionUseCase
import com.liebeblack.divtrack.domain.usecase.ObserveRatesUseCase
import com.liebeblack.divtrack.domain.usecase.ObserveSettingsUseCase
import com.liebeblack.divtrack.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Calculadora bidireccional.
 *
 * El cálculo es **automático en cada tecla**: `inputText` es un `MutableStateFlow` y el
 * estado de UI se deriva con `combine` + `stateIn(WhileSubscribed)`. No hay botón "Calcular"
 * y no se recalcula nada mientras nadie observa la pantalla (sin trabajo en background).
 *
 * El saneado del texto (`NumberParsing.sanitizeAmountInput`) hace que pegar "1.234,56" o
 * escribir "1234,5" funcione sin que el usuario pelee con el formato.
 */
@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val observeRates: ObserveRatesUseCase,
    private val observeSettings: ObserveSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val calculateConversion: CalculateConversionUseCase,
) : ViewModel() {

    private val inputText = MutableStateFlow("")
    private val direction = MutableStateFlow(ConversionDirection.USD_TO_BS)
    private val selectedSource = MutableStateFlow<RateSource?>(null)
    private val igtfEnabled = MutableStateFlow<Boolean?>(null)
    private val availableRates = MutableStateFlow<List<ExchangeRate>>(emptyList())

    private val _effects = MutableSharedFlow<CalculatorEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<CalculatorEffect> = _effects.asSharedFlow()

    val state: StateFlow<CalculatorUiState> = combine(
        inputText,
        direction,
        selectedSource,
        igtfEnabled,
        availableRates,
    ) { input, currentDirection, source, igtf, rates ->
        buildState(
            input = input,
            direction = currentDirection,
            requestedSource = source,
            igtf = igtf,
            rates = rates,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CalculatorUiState(igtfRateText = AppConstants.IGTF_LABEL),
    )

    init {
        viewModelScope.launch {
            observeRates().collect { rates -> availableRates.value = rates }
        }

        viewModelScope.launch {
            // Las preferencias inicializan la calculadora; si el usuario ya eligió algo en
            // esta sesión, su elección manda (el valor no nulo gana).
            observeSettings().collect { settings ->
                if (selectedSource.value == null) selectedSource.value = settings.defaultSource
                if (igtfEnabled.value == null) igtfEnabled.value = settings.igtfEnabled
            }
        }
    }

    fun onIntent(intent: CalculatorIntent) {
        when (intent) {
            is CalculatorIntent.AmountChanged -> {
                inputText.value = NumberParsing.sanitizeAmountInput(intent.value)
            }

            is CalculatorIntent.SelectSource -> selectedSource.value = intent.source

            CalculatorIntent.SwapDirection -> {
                direction.value = when (direction.value) {
                    ConversionDirection.USD_TO_BS -> ConversionDirection.BS_TO_USD
                    ConversionDirection.BS_TO_USD -> ConversionDirection.USD_TO_BS
                }
            }

            is CalculatorIntent.ToggleIgtf -> {
                igtfEnabled.value = intent.enabled
                viewModelScope.launch { updateSettings.setIgtfEnabled(intent.enabled) }
            }

            CalculatorIntent.ClearAmount -> inputText.value = ""

            CalculatorIntent.CopyBreakdown -> copyBreakdown()
        }
    }

    private fun copyBreakdown() {
        val current = state.value
        if (!current.canCopy) return

        viewModelScope.launch {
            _effects.emit(
                CalculatorEffect.CopyToClipboard(
                    summary = BreakdownSummary(
                        direction = current.direction,
                        source = current.selectedSource,
                        amountText = current.inputText,
                        rateText = current.selectedRateText,
                        igtfEnabled = current.igtfEnabled,
                        igtfText = current.igtfBsText,
                        netUsdText = current.netUsdText,
                        netBsText = current.netBsText,
                        totalBsText = current.totalBsText,
                        totalUsdText = current.totalUsdText,
                    ),
                ),
            )
        }
    }

    private fun buildState(
        input: String,
        direction: ConversionDirection,
        requestedSource: RateSource?,
        igtf: Boolean?,
        rates: List<ExchangeRate>,
    ): CalculatorUiState {
        val options = rates.map { rate ->
            RateOptionUi(
                source = rate.source,
                value = rate.value,
            )
        }

        // La preferencia del usuario manda **solo si esa tasa existe hoy**: si el proveedor
        // no la ha publicado, caemos en la que sí está en lugar de dejar la calculadora
        // mostrando "sin tasas" con la otra disponible a un toque de distancia.
        val source = requestedSource
            ?.takeIf { requested -> options.any { option -> option.source == requested } }
            ?: options.firstOrNull()?.source
            ?: RateSource.OFICIAL
        // `options` sale de `rates` en el mismo orden, así que basta una búsqueda.
        val selectedRate = options.firstOrNull { it.source == source }?.value

        val igtfEnabledValue = igtf ?: false
        val conversion: Conversion? = calculateConversion(
            direction = direction,
            source = source,
            rate = selectedRate,
            rawAmount = input,
            igtfEnabled = igtfEnabledValue,
        )

        return CalculatorUiState(
            inputText = input,
            direction = direction,
            selectedSource = source,
            rateOptions = options,
            selectedRateText = selectedRate?.let { CurrencyFormatters.amount(it) } ?: "—",
            igtfEnabled = igtfEnabledValue,
            igtfRateText = AppConstants.IGTF_LABEL,
            netUsdText = conversion?.let { CurrencyFormatters.amount(it.netUsd) } ?: "—",
            netBsText = conversion?.let { CurrencyFormatters.amount(it.netBs) } ?: "—",
            igtfBsText = conversion?.let { CurrencyFormatters.amount(it.igtfBs) } ?: "—",
            totalBsText = conversion?.let { CurrencyFormatters.amount(it.totalBs) } ?: "—",
            totalUsdText = conversion?.let { CurrencyFormatters.amount(it.totalUsd) } ?: "—",
            canCopy = conversion?.hasAmount == true,
            hasRate = selectedRate != null,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
