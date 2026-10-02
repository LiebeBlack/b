package com.liebeblack.divtrack.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.liebeblack.divtrack.presentation.R
import com.liebeblack.divtrack.presentation.theme.Spacing
import com.liebeblack.divtrack.presentation.theme.tabular

/**
 * Brecha entre paralelo y oficial. Es el indicador que de verdad se mira en Venezuela,
 * así que tiene tarjeta propia bajo las dos tasas.
 */
@Composable
fun SpreadChip(
    percentText: String?,
    absoluteText: String?,
    modifier: Modifier = Modifier,
) {
    // Copias locales: `stringResource` con formato recibe `Any` (no `Any?`), así que el
    // chequeo de nulos tiene que hacer smart cast sobre variables, no sobre parámetros.
    val percent = percentText
    val absolute = absoluteText

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.spread_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (percent != null && absolute != null) {
                    stringResource(R.string.spread_value, percent, absolute)
                } else {
                    stringResource(R.string.spread_unavailable)
                },
                style = MaterialTheme.typography.titleMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}
