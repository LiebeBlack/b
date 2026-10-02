package com.liebeblack.divtrack.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.presentation.R
import com.liebeblack.divtrack.presentation.common.CollectEffects
import com.liebeblack.divtrack.presentation.common.asString
import com.liebeblack.divtrack.presentation.common.labelRes
import com.liebeblack.divtrack.presentation.common.openNetworkSettings
import com.liebeblack.divtrack.presentation.components.ErrorState
import com.liebeblack.divtrack.presentation.components.LoadingState
import com.liebeblack.divtrack.presentation.components.RateCard
import com.liebeblack.divtrack.presentation.components.SpreadChip
import com.liebeblack.divtrack.presentation.theme.DivTrackThemeTokens
import com.liebeblack.divtrack.presentation.theme.Spacing

/**
 * Ruta: único punto donde se inyecta el ViewModel y se resuelven los efectos de un solo uso.
 * Todo lo que hay debajo son composables sin estado que reciben datos y lambdas.
 */
@Composable
fun DashboardRoute(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Referencia recordada a propósito: `viewModel::onIntent` crea un objeto nuevo en cada
    // recomposición, y eso basta para que Compose considere "distinto" el parámetro y no
    // pueda saltarse el trabajo de las pantallas que lo reciben.
    val onIntent = remember(viewModel) { viewModel::onIntent }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is DashboardEffect.ShowMessage ->
                snackbarHostState.showSnackbar(message = effect.text.asString(context))
        }
    }

    DashboardScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (DashboardIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DivTrackThemeTokens.colors
    val context = LocalContext.current

    // El texto del error se resuelve una vez por estado, no en cada tarjeta.
    val errorMessage = state.errorText?.asString(context)
    val onRetry = remember(onIntent) { { onIntent(DashboardIntent.Retry) } }
    val onRefresh = remember(onIntent) { { onIntent(DashboardIntent.Refresh) } }
    val onOpenNetworkSettings = remember(context) { { context.openNetworkSettings() } }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.isRefreshing && !state.isLoading,
                    ) {
                        if (state.isRefreshing || state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.action_refresh),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 720.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.lg,
                    end = Spacing.lg,
                    top = padding.calculateTopPadding() + Spacing.sm,
                    bottom = padding.calculateBottomPadding() + Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                if (state.isLoading) {
                    item(key = "loading") {
                        LoadingState(message = stringResource(R.string.loading_rates))
                    }
                } else {
                    // El snackbar se va solo; este aviso permanece mientras no haya conexión,
                    // que es lo que el usuario necesita saber mientras mira tasas guardadas.
                    if (state.isOffline && state.hasData) {
                        item(key = "offline") {
                            OfflineBanner(
                                message = errorMessage
                                    ?: stringResource(R.string.msg_offline_showing_cache),
                            )
                        }
                    }

                    if (state.hasStaleData) {
                        item(key = "stale") {
                            OfflineBanner(message = stringResource(R.string.msg_stale_data))
                        }
                    }

                    if (!state.hasData) {
                        item(key = "error") {
                            ErrorState(
                                message = errorMessage
                                    ?: stringResource(R.string.msg_offline_no_data),
                                retryLabel = stringResource(R.string.action_retry),
                                onRetry = onRetry,
                                secondaryLabel = if (state.isConnectivityProblem) {
                                    stringResource(R.string.action_open_network_settings)
                                } else {
                                    null
                                },
                                onSecondaryAction = if (state.isConnectivityProblem) {
                                    onOpenNetworkSettings
                                } else {
                                    null
                                },
                            )
                        }
                    }

                    // Las claves estables evitan recomponer la tarjeta que no cambió.
                    items(items = state.rates, key = { rate -> rate.source.key }) { rate ->
                        val isOfficial = rate.source == RateSource.OFICIAL
                        RateCard(
                            title = stringResource(
                                R.string.rate_card_title,
                                stringResource(rate.source.labelRes()),
                            ),
                            valueText = rate.valueText,
                            trend = rate.trend,
                            deltaText = rate.deltaText,
                            providerText = stringResource(R.string.rate_provider, rate.providerText),
                            updatedAtText = rate.updatedAtText?.let { updated ->
                                stringResource(R.string.rate_updated_at, updated)
                            },
                            isStale = rate.isStale,
                            accentColor = if (isOfficial) colors.officialAccent else colors.parallelAccent,
                            isHero = isOfficial,
                        )
                    }

                    if (state.hasData) {
                        item(key = "spread") {
                            SpreadChip(
                                percentText = state.spreadPercentText,
                                absoluteText = state.spreadAbsoluteText,
                            )
                        }

                        item(key = "legal") {
                            Text(
                                text = stringResource(R.string.dashboard_disclaimer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
