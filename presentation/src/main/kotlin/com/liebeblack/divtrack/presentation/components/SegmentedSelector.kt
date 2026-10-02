package com.liebeblack.divtrack.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Selector de elección única que reorganiza sus opciones en varias filas según el ancho.
 *
 * Un solo componente cubre los usos de la app (fuente, tema e intervalo), manteniendo
 * objetivos táctiles cómodos en teléfonos estrechos y con texto ampliado.
 *
 * `label` es `@Composable` porque cada llamada resuelve un recurso de texto
 * (`stringResource`), y eso solo se puede hacer desde contexto de composición.
 */
@Composable
fun <T> SegmentedSelector(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}
