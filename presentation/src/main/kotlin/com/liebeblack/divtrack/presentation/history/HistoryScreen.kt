package com.liebeblack.divtrack.presentation.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liebeblack.divtrack.core.common.utils.CurrencyFormatters
import com.liebeblack.divtrack.domain.model.HistoryRange
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.TrendDirection
import com.liebeblack.divtrack.presentation.R
import com.liebeblack.divtrack.presentation.common.CollectEffects
import com.liebeblack.divtrack.presentation.common.asString
import com.liebeblack.divtrack.presentation.common.labelRes
import com.liebeblack.divtrack.presentation.components.ChartSeriesData
import com.liebeblack.divtrack.presentation.components.EmptyState
import com.liebeblack.divtrack.presentation.components.LoadingState
import com.liebeblack.divtrack.presentation.components.RateLineChart
import com.liebeblack.divtrack.presentation.components.SegmentedSelector
import com.liebeblack.divtrack.presentation.components.TrendArrow
import com.liebeblack.divtrack.presentation.theme.DivTrackThemeTokens
import com.liebeblack.divtrack.presentation.theme.Spacing

@Composable
fun HistoryRoute(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is HistoryEffect.ShowMessage ->
                snackbarHostState.showSnackbar(message = effect.text.asString(context))
        }
    }

    HistoryScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (HistoryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DivTrackThemeTokens.colors

    val chartSeries = remember(state.series) {
        state.series.map { serie ->
            ChartSeriesData(
                label = serie.source.key,
                color = if (serie.source == RateSource.OFICIAL) colors.chartOficial else colors.chartParalelo,
                values = serie.values,
            )
        }
    }

    val pointCount = remember(state.series) { state.series.maxOfOrNull { it.values.size } ?: 0 }

    // derivedStateOf para el crosshair: al arrastrar solo se recalcula el detalle del punto
    // seleccionado, no la pantalla completa ni las estadísticas del rango.
    val latestState by rememberUpdatedState(state)
    val selectionDetail by remember {
        derivedStateOf {
            val snapshot = latestState
            val index = snapshot.selectionIndex ?: return@derivedStateOf null
            if (index !in 0 until pointCount) return@derivedStateOf null
            val label = snapshot.selectionDateLabel ?: return@derivedStateOf null
            val oficial = snapshot.valueAt(index, RateSource.OFICIAL)
            val paralelo = snapshot.valueAt(index, RateSource.PARALELO)
            buildString {
                append(label)
                if (oficial != null) {
                    append(" · ")
                    append(CurrencyFormatters.bolivars(oficial.toDouble()))
                }
                if (paralelo != null) {
                    append(" · ")
                    append(CurrencyFormatters.bolivars(paralelo.toDouble()))
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.history_title)) },
                actions = {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = Spacing.sm),
                            strokeWidth = 2.dp,
                        )
                    }
                    IconButton(onClick = { onIntent(HistoryIntent.SyncHistory) }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.action_sync_history),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Spacing.lg,
                    end = Spacing.lg,
                    top = padding.calculateTopPadding() + Spacing.sm,
                    bottom = padding.calculateBottomPadding() + Spacing.xl,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SegmentedSelector(
                options = HistoryRange.entries.toList(),
                selected = state.selectedRange,
                onSelect = { range -> onIntent(HistoryIntent.SelectRange(range)) },
                label = { range -> stringResource(range.labelRes()) },
                modifier = Modifier.fillMaxWidth(),
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    when {
                        state.isLoading && !state.hasData -> LoadingState()
                        !state.hasData -> EmptyState(
                            title = stringResource(R.string.history_empty_title),
                            message = stringResource(R.string.history_empty_message),
                        )

                        else -> {
                            LegendRow()
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            RateLineChart(
                                series = chartSeries,
                                firstXLabel = state.firstXLabel,
                                lastXLabel = state.lastXLabel,
                                selectedIndex = state.selectionIndex,
                                onSelectIndex = { index ->
                                    onIntent(HistoryIntent.SelectIndex(index))
                                },
                                formatValue = { value -> CurrencyFormatters.amount(value.toDouble()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = selectionDetail
                                    ?: stringResource(R.string.history_drag_hint),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            state.stats.forEach { stat ->
                key(stat.source.key) {
                    StatCard(stat = stat)
                }
            }
        }
    }
}

@Composable
private fun LegendRow(modifier: Modifier = Modifier) {
    val colors = DivTrackThemeTokens.colors
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(color = colors.chartOficial, label = stringResource(R.string.source_oficial_long))
        Spacer(modifier = Modifier.width(Spacing.lg))
        LegendDot(color = colors.chartParalelo, label = stringResource(R.string.source_paralelo_long))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = color,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.size(10.dp),
        ) {}
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatCard(stat: HistoryStatUi, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(stat.source.labelRes()),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (stat.changePercentText != null) {
                    TrendArrow(direction = stat.trend, arrowSize = 11.dp)
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = stat.changePercentText,
                        style = MaterialTheme.typography.labelMedium,
                        color = trendColor(stat.trend),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatValue(label = stringResource(R.string.history_stat_latest), value = stat.latestText)
                StatValue(label = stringResource(R.string.history_stat_min), value = stat.minText)
                StatValue(label = stringResource(R.string.history_stat_max), value = stat.maxText)
            }
        }
    }
}

@Composable
private fun StatValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Color de la variación del rango: los mismos semánticos que las flechas del panel. */
@Composable
private fun trendColor(direction: TrendDirection): Color {
    val colors = DivTrackThemeTokens.colors
    return when (direction) {
        TrendDirection.UP -> colors.trendUp
        TrendDirection.DOWN -> colors.trendDown
        TrendDirection.FLAT -> colors.trendFlat
    }
}
