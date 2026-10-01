package com.liebeblack.divtrack.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liebeblack.divtrack.core.common.result.Result
import com.liebeblack.divtrack.core.common.utils.CurrencyFormatters
import com.liebeblack.divtrack.domain.model.HistoryRange
import com.liebeblack.divtrack.domain.model.RateHistoryPoint
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.TrendDirection
import com.liebeblack.divtrack.domain.usecase.ObserveHistoryUseCase
import com.liebeblack.divtrack.domain.usecase.SyncHistoryUseCase
import com.liebeblack.divtrack.presentation.R
import com.liebeblack.divtrack.presentation.common.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Histórico YTD.
 *
 * - Se re-observa la serie cada vez que cambia el rango (`collectLatest` cancela la
 *   observación anterior: nunca hay dos consultas vivas a la vez).
 * - La importación del histórico respeta un TTL de 24 h en el dominio, así que abrir esta
 *   pantalla no castiga la red; el botón de la barra fuerza la actualización.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val observeHistory: ObserveHistoryUseCase,
    private val syncHistory: SyncHistoryUseCase,
) : ViewModel() {

    private val selectedRange = MutableStateFlow(HistoryRange.default)
    private val points = MutableStateFlow<List<RateHistoryPoint>>(emptyList())
    private val isLoading = MutableStateFlow(true)
    private val isSyncing = MutableStateFlow(false)
    private val selectionIndex = MutableStateFlow<Int?>(null)

    private val _effects = MutableSharedFlow<HistoryEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<HistoryEffect> = _effects.asSharedFlow()

    val state: StateFlow<HistoryUiState> = combine(
        selectedRange,
        points,
        isLoading,
        isSyncing,
        selectionIndex,
    ) { range, history, loading, syncing, selection ->
        buildState(
            range = range,
            history = history,
            loading = loading,
            syncing = syncing,
            selection = selection,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HistoryUiState(),
    )

    private var rangeCollection: Job? = null

    init {
        observeRange()
        syncHistoryFromRemote(force = false)
    }

    fun onIntent(intent: HistoryIntent) {
        when (intent) {
            is HistoryIntent.SelectRange -> {
                if (selectedRange.value != intent.range) {
                    selectedRange.value = intent.range
                    selectionIndex.value = null
                    observeRange()
                }
            }

            HistoryIntent.SyncHistory -> syncHistoryFromRemote(force = true)

            is HistoryIntent.SelectIndex -> selectionIndex.value = intent.index
        }
    }

    private fun observeRange() {
        rangeCollection?.cancel()
        rangeCollection = viewModelScope.launch {
            selectedRange.collectLatest { range ->
                isLoading.value = true
                observeHistory(range).collect { history ->
                    points.value = history
                    isLoading.value = false
                }
            }
        }
    }

    /**
     * Importa el histórico. El nombre NO puede ser `syncHistory`: colisionaría con la
     * propiedad del caso de uso inyectada y el `when` de abajo acabaría analizando la
     * función de esta misma clase (una llamada recursiva) en lugar del caso de uso.
     */
    private fun syncHistoryFromRemote(force: Boolean) {
        viewModelScope.launch {
            isSyncing.value = true
            when (val result = syncHistory(force)) {
                is Result.Success -> Unit

                is Result.Error -> _effects.emit(
                    HistoryEffect.ShowMessage(UiText.Res(R.string.msg_history_offline)),
                )

                Result.Loading -> Unit
            }
            isSyncing.value = false
        }
    }

    private fun buildState(
        range: HistoryRange,
        history: List<RateHistoryPoint>,
        loading: Boolean,
        syncing: Boolean,
        selection: Int?,
    ): HistoryUiState {
        if (history.isEmpty()) {
            return HistoryUiState(
                selectedRange = range,
                isLoading = loading,
                isSyncing = syncing,
            )
        }

        // Línea de tiempo común: los índices del gráfico son días, no posiciones de lista.
        val timeline = history.map { it.epochDay }.distinct().sorted()
        val bySourceAndDay = history.associateBy { point -> point.source to point.epochDay }

        val series = RateSource.ordered().map { source ->
            HistorySeriesUi(
                source = source,
                values = timeline.map { day -> bySourceAndDay[source to day]?.value?.toFloat() },
            )
        }

        val stats = RateSource.ordered().mapNotNull { source ->
            val values = series.firstOrNull { it.source == source }?.values.orEmpty()
            val present = values.filterNotNull()
            if (present.isEmpty()) {
                null
            } else {
                val first = present.first()
                val last = present.last()
                val change = if (first != 0f) ((last - first) / first) * 100.0 else null
                HistoryStatUi(
                    source = source,
                    latestText = CurrencyFormatters.bolivars(last.toDouble()),
                    minText = CurrencyFormatters.bolivars(present.min().toDouble()),
                    maxText = CurrencyFormatters.bolivars(present.max().toDouble()),
                    changePercentText = change?.let { CurrencyFormatters.percent(it) },
                    trend = when {
                        change == null -> TrendDirection.FLAT
                        change > 0 -> TrendDirection.UP
                        change < 0 -> TrendDirection.DOWN
                        else -> TrendDirection.FLAT
                    },
                )
            }
        }

        val selectionLabel = selection
            ?.takeIf { it in timeline.indices }
            ?.let { index -> CurrencyFormatters.shortDate(LocalDate.ofEpochDay(timeline[index])) }

        return HistoryUiState(
            selectedRange = range,
            isLoading = loading,
            isSyncing = syncing,
            series = series,
            stats = stats,
            firstXLabel = timeline.firstOrNull()?.let { day ->
                CurrencyFormatters.shortDate(LocalDate.ofEpochDay(day))
            },
            lastXLabel = timeline.lastOrNull()?.let { day ->
                CurrencyFormatters.shortDate(LocalDate.ofEpochDay(day))
            },
            selectionIndex = selection,
            selectionDateLabel = selectionLabel,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
