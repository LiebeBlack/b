package com.liebeblack.divtrack.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.ThemeMode
import com.liebeblack.divtrack.presentation.R
import com.liebeblack.divtrack.presentation.common.CollectEffects
import com.liebeblack.divtrack.presentation.common.asString
import com.liebeblack.divtrack.presentation.common.labelRes
import com.liebeblack.divtrack.presentation.components.SegmentedSelector
import com.liebeblack.divtrack.presentation.components.SettingsSwitchRow
import com.liebeblack.divtrack.presentation.theme.Spacing

@Composable
fun SettingsRoute(
    appVersion: String,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is SettingsEffect.ShowMessage ->
                snackbarHostState.showSnackbar(message = effect.text.asString(context))
        }
    }

    SettingsScreen(
        state = state,
        appVersion = appVersion,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    appVersion: String = "",
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.settings_title)) })
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
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SettingsCard(
                title = stringResource(R.string.settings_theme),
                helper = stringResource(R.string.settings_theme_helper),
            ) {
                SegmentedSelector(
                    options = ThemeMode.entries.toList(),
                    selected = state.themeMode,
                    onSelect = { mode -> onIntent(SettingsIntent.SelectTheme(mode)) },
                    label = { mode -> stringResource(mode.labelRes()) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SettingsCard(
                title = stringResource(R.string.settings_default_source),
                helper = stringResource(R.string.settings_default_source_helper),
            ) {
                SegmentedSelector(
                    options = RateSource.ordered(),
                    selected = state.defaultSource,
                    onSelect = { source -> onIntent(SettingsIntent.SelectDefaultSource(source)) },
                    label = { source -> stringResource(source.labelRes()) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SettingsSwitchRow(
                title = stringResource(R.string.settings_igtf_default),
                helper = stringResource(R.string.settings_igtf_default_helper),
                checked = state.igtfEnabled,
                onCheckedChange = { enabled -> onIntent(SettingsIntent.SetIgtfDefault(enabled)) },
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_auto_sync),
                helper = stringResource(R.string.settings_auto_sync_helper),
                checked = state.autoSyncEnabled,
                onCheckedChange = { enabled -> onIntent(SettingsIntent.SetAutoSync(enabled)) },
            )

            // La frecuencia solo se ofrece si la sincronización está encendida: un control
            // activo que no hace nada es peor que no mostrarlo.
            if (state.autoSyncEnabled) {
                SettingsCard(title = stringResource(R.string.settings_interval)) {
                    SegmentedSelector(
                        options = SyncIntervalOptions,
                        selected = state.syncIntervalMinutes,
                        onSelect = { minutes -> onIntent(SettingsIntent.SetSyncInterval(minutes)) },
                        label = { minutes ->
                            stringResource(R.string.settings_interval_minutes, minutes)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            AboutCard(appVersion = appVersion)
        }
    }
}

/** Tarjeta de sección con título, explicación opcional y contenido. */
@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    helper: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SectionTitle(text = title)
            if (helper != null) {
                SectionBody(text = helper)
            }
            content()
        }
    }
}

@Composable
private fun AboutCard(appVersion: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SectionTitle(text = stringResource(R.string.settings_about))

            Text(
                text = stringResource(
                    R.string.settings_version,
                    appVersion.ifBlank { NOT_AVAILABLE },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            SectionTitle(text = stringResource(R.string.settings_providers))
            SectionBody(text = stringResource(R.string.settings_providers_value))

            SectionTitle(text = stringResource(R.string.settings_disclaimer_title))
            SectionBody(text = stringResource(R.string.settings_disclaimer_value))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SectionBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val NOT_AVAILABLE = "—"
