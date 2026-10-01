package com.liebeblack.divtrack.presentation.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.presentation.R
import com.liebeblack.divtrack.presentation.common.CollectEffects
import com.liebeblack.divtrack.presentation.common.asString
import com.liebeblack.divtrack.presentation.common.copyToClipboard
import com.liebeblack.divtrack.presentation.common.labelRes
import com.liebeblack.divtrack.presentation.components.AmountField
import com.liebeblack.divtrack.presentation.components.IgtfSwitchRow
import com.liebeblack.divtrack.presentation.components.SegmentedSelector
import com.liebeblack.divtrack.presentation.theme.DivTrackThemeTokens
import com.liebeblack.divtrack.presentation.theme.Spacing
import com.liebeblack.divtrack.presentation.theme.tabular

/** Etiqueta del portapapeles que verán otras apps al pegar. */
private const val CLIPBOARD_LABEL = "DivTrack"

@Composable
fun CalculatorRoute(viewModel: CalculatorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is CalculatorEffect.CopyToClipboard -> {
                context.copyToClipboard(
                    label = CLIPBOARD_LABEL,
                    text = buildBreakdownText(context, effect.summary),
                )
                snackbarHostState.showSnackbar(message = context.getString(R.string.copy_done))
            }

            is CalculatorEffect.ShowMessage ->
                snackbarHostState.showSnackbar(message = effect.text.asString(context))
        }
    }

    CalculatorScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (CalculatorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DivTrackThemeTokens.colors

    // --- derivedStateOf: uso crítico, como pide el spec ---
    //
    // Objetivo: al teclear no se recompone la pantalla entera, solo el bloque que muestra el
    // equivalente en la otra moneda. `rememberUpdatedState` es imprescindible: sin él, el
    // bloque `derivedStateOf` capturaría el `state` de la primera composición y quedaría
    // obsoleto (bug clásico, silencioso y difícil de ver).
    val latestState by rememberUpdatedState(state)
    val mirroredAmount by remember {
        derivedStateOf {
            val snapshot = latestState
            when {
                !snapshot.isAmountValid -> ""
                snapshot.isUsdToBs -> snapshot.totalBsText
                else -> snapshot.totalUsdText
            }
        }
    }

    val accentColor = if (state.selectedSource == RateSource.OFICIAL) {
        colors.officialAccent
    } else {
        colors.parallelAccent
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.calculator_title)) },
                actions = {
                    IconButton(onClick = { onIntent(CalculatorIntent.ClearAmount) }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.action_clear),
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
            // Solo se ofrecen las tasas que existen de verdad en Room. Si un proveedor no
            // ha publicado el oficial todavía, no tiene sentido dejar elegirlo para acabar
            // viendo "sin tasas disponibles".
            SegmentedSelector(
                options = state.rateOptions.map { option -> option.source }
                    .ifEmpty { RateSource.ordered() },
                selected = state.selectedSource,
                onSelect = { source -> onIntent(CalculatorIntent.SelectSource(source)) },
                label = { source -> stringResource(source.labelRes()) },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.calculator_rate_line, state.selectedRateText),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AmountField(
                value = state.inputText,
                onValueChange = { value -> onIntent(CalculatorIntent.AmountChanged(value)) },
                label = if (state.isUsdToBs) {
                    stringResource(R.string.calculator_amount_usd)
                } else {
                    stringResource(R.string.calculator_amount_bs)
                },
                prefix = if (state.isUsdToBs) "$" else "Bs.",
                accentColor = accentColor,
            )

            TextButton(onClick = { onIntent(CalculatorIntent.SwapDirection) }) {
                Text(text = stringResource(R.string.action_swap_direction))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    ResultRow(
                        label = if (state.isUsdToBs) {
                            stringResource(R.string.result_mirror_bs)
                        } else {
                            stringResource(R.string.result_mirror_usd)
                        },
                        value = mirroredAmount.ifEmpty { "—" },
                        emphasize = true,
                    )
                    ResultRow(
                        label = stringResource(R.string.result_net_usd),
                        value = state.netUsdText,
                    )
                    ResultRow(
                        label = stringResource(R.string.result_net_bs),
                        value = state.netBsText,
                    )
                    ResultRow(
                        label = stringResource(R.string.result_igtf, state.igtfRateText),
                        value = state.igtfBsText,
                    )
                    ResultRow(
                        label = stringResource(R.string.result_total_usd),
                        value = state.totalUsdText,
                    )
                }
            }

            IgtfSwitchRow(
                checked = state.igtfEnabled,
                onCheckedChange = { enabled -> onIntent(CalculatorIntent.ToggleIgtf(enabled)) },
                igtfRateText = state.igtfRateText,
            )

            Button(
                onClick = { onIntent(CalculatorIntent.CopyBreakdown) },
                enabled = state.canCopy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Filled.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(text = stringResource(R.string.action_copy_breakdown))
            }

            val hintText = when {
                !state.hasRate -> stringResource(R.string.calculator_no_rate)
                !state.isAmountValid -> stringResource(R.string.calculator_hint)
                else -> null
            }

            if (hintText != null) {
                Text(
                    text = hintText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.hasRate) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = if (emphasize) {
                MaterialTheme.typography.headlineSmall.tabular()
            } else {
                MaterialTheme.typography.titleMedium.tabular()
            },
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
