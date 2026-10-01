package com.liebeblack.divtrack.presentation.history

import androidx.compose.runtime.Immutable
import com.liebeblack.divtrack.domain.model.HistoryRange
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.TrendDirection
import com.liebeblack.divtrack.presentation.common.UiText

/**
 * Una serie del gráfico. `values` está alineado con la línea de tiempo común del rango:
 * un `null` significa "ese día no hay dato de esta fuente" y el gráfico deja un hueco en
 * lugar de inventarse una interpolación.
 */
@Immutable
data class HistorySeriesUi(
    val source: RateSource,
    val values: List<Float?>,
)

/** Estadísticas del rango para una fuente. */
@Immutable
data class HistoryStatUi(
    val source: RateSource,
    val latestText: String,
    val minText: String,
    val maxText: String,
    val changePercentText: String?,
    val trend: TrendDirection,
)

@Immutable
data class HistoryUiState(
    val selectedRange: HistoryRange = HistoryRange.default,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val series: List<HistorySeriesUi> = emptyList(),
    val stats: List<HistoryStatUi> = emptyList(),
    val firstXLabel: String? = null,
    val lastXLabel: String? = null,
    val selectionIndex: Int? = null,
    val selectionDateLabel: String? = null,
) {
    val hasData: Boolean get() = series.any { serie -> serie.values.any { it != null } }

    fun valueAt(index: Int, source: RateSource): Float? =
        series.firstOrNull { it.source == source }?.values?.getOrNull(index)
}

sealed interface HistoryIntent {

    data class SelectRange(val range: HistoryRange) : HistoryIntent

    data object SyncHistory : HistoryIntent

    /** `null` limpia el crosshair. */
    data class SelectIndex(val index: Int?) : HistoryIntent
}

sealed interface HistoryEffect {

    data class ShowMessage(val text: UiText) : HistoryEffect
}
