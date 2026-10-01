package com.liebeblack.divtrack.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Una serie del gráfico. `null` representa un día sin dato (se dibuja un hueco, no una mentira). */
@Immutable
data class ChartSeriesData(
    val label: String,
    val color: Color,
    val values: List<Float?>,
)

/**
 * Gráfico de líneas dibujado en un único `Canvas`: sin librerías de charts y sin asignaciones
 * por fotograma más allá de la construcción del `Path` cuando cambian los datos.
 *
 * El crosshair se resuelve con `pointerInput` (tap + arrastre) y el índice seleccionado lo
 * guarda la pantalla, que es quien deriva el detalle con `derivedStateOf`.
 */
@Composable
fun RateLineChart(
    series: List<ChartSeriesData>,
    firstXLabel: String?,
    lastXLabel: String?,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onSelectIndex: (Int?) -> Unit = {},
    formatValue: (Float) -> String = { it.roundToInt().toString() },
) {
    val textMeasurer = rememberTextMeasurer()
    val axisTextStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val crosshairColor = MaterialTheme.colorScheme.onSurfaceVariant

    val pointCount = remember(series) { series.maxOfOrNull { it.values.size } ?: 0 }
    val bounds = remember(series) { computeBounds(series) }
    val normalized = remember(series) { series.map { serie -> normalize(serie.values, bounds) } }

    val maxLabel = remember(bounds, textMeasurer, axisTextStyle, formatValue) {
        bounds?.let { textMeasurer.measure(formatValue(it.max), axisTextStyle) }
    }
    val minLabel = remember(bounds, textMeasurer, axisTextStyle, formatValue) {
        bounds?.let { textMeasurer.measure(formatValue(it.min), axisTextStyle) }
    }
    val firstXLabelLayout = remember(firstXLabel, textMeasurer, axisTextStyle) {
        firstXLabel?.let { textMeasurer.measure(it, axisTextStyle) }
    }
    val lastXLabelLayout = remember(lastXLabel, textMeasurer, axisTextStyle) {
        lastXLabel?.let { textMeasurer.measure(it, axisTextStyle) }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pointCount) {
                    detectTapGestures { offset ->
                        onSelectIndex(indexAt(offset.x, size.width.toFloat(), pointCount, plotPaddingPx()))
                    }
                }
                .pointerInput(pointCount) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onSelectIndex(indexAt(offset.x, size.width.toFloat(), pointCount, plotPaddingPx()))
                        },
                        onDragEnd = {},
                        onDragCancel = {},
                        onDrag = { change, _ ->
                            change.consume()
                            onSelectIndex(indexAt(change.position.x, size.width.toFloat(), pointCount, plotPaddingPx()))
                        },
                    )
                },
        ) {
            if (bounds == null || pointCount == 0) return@Canvas

            val padding = plotPaddingPx()
            val plotWidth = (size.width - padding * 2f).coerceAtLeast(1f)
            val plotHeight = (size.height - padding * 2f).coerceAtLeast(1f)

            // Rejilla: tres líneas discontinuas. Sin animación, coste cero.
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            listOf(0f, 0.5f, 1f).forEach { fraction ->
                val y = padding + plotHeight * fraction
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(padding + plotWidth, y),
                    strokeWidth = 1f,
                    pathEffect = dash,
                )
            }

            // Relleno degradado de la última serie (la de referencia) para dar profundidad.
            val referencePoints = normalized.lastOrNull()
            if (referencePoints != null) {
                val fillPath = Path()
                var started = false
                referencePoints.forEachIndexed { index, value ->
                    val x = padding + plotWidth * xFraction(index, pointCount)
                    if (value == null) {
                        started = false
                        return@forEachIndexed
                    }
                    val y = padding + plotHeight * (1f - value)
                    if (!started) {
                        fillPath.moveTo(x, y)
                        started = true
                    } else {
                        fillPath.lineTo(x, y)
                    }
                }
                if (started) {
                    fillPath.lineTo(padding + plotWidth, padding + plotHeight)
                    fillPath.lineTo(padding, padding + plotHeight)
                    fillPath.close()
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                series.last().color.copy(alpha = 0.28f),
                                series.last().color.copy(alpha = 0.02f),
                            ),
                            startY = padding,
                            endY = padding + plotHeight,
                        ),
                    )
                }
            }

            // Líneas de cada serie.
            series.forEachIndexed { serieIndex, serie ->
                val values = normalized.getOrNull(serieIndex) ?: return@forEachIndexed
                val path = Path()
                var started = false
                values.forEachIndexed { index, value ->
                    val x = padding + plotWidth * xFraction(index, pointCount)
                    if (value == null) {
                        started = false
                        return@forEachIndexed
                    }
                    val y = padding + plotHeight * (1f - value)
                    if (!started) {
                        path.moveTo(x, y)
                        started = true
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = serie.color,
                    style = Stroke(
                        width = 2.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }

            // Crosshair del índice seleccionado.
            if (selectedIndex != null && selectedIndex in 0 until pointCount) {
                val x = padding + plotWidth * xFraction(selectedIndex, pointCount)
                drawLine(
                    color = crosshairColor.copy(alpha = 0.55f),
                    start = Offset(x, padding),
                    end = Offset(x, padding + plotHeight),
                    strokeWidth = 1.2.dp.toPx(),
                )
                normalized.forEachIndexed { serieIndex, values ->
                    val value = values.getOrNull(selectedIndex) ?: return@forEachIndexed
                    val y = padding + plotHeight * (1f - value)
                    drawCircle(
                        color = series.getOrNull(serieIndex)?.color ?: crosshairColor,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y),
                    )
                }
            }

            // Etiquetas: máximo y mínimo a la derecha, extremos del eje X abajo.
            maxLabel?.let { layout ->
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = padding + plotWidth - layout.size.width,
                        y = padding,
                    ),
                )
            }
            minLabel?.let { layout ->
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = padding + plotWidth - layout.size.width,
                        y = padding + plotHeight - layout.size.height,
                    ),
                )
            }
            firstXLabelLayout?.let { layout ->
                drawText(textLayoutResult = layout, topLeft = Offset(padding, size.height - layout.size.height))
            }
            lastXLabelLayout?.let { layout ->
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = padding + plotWidth - layout.size.width,
                        y = size.height - layout.size.height,
                    ),
                )
            }
        }
    }
}

// --- Cálculos puros, fuera del scope de dibujo ---

@Immutable
private data class ChartBounds(val min: Float, val max: Float)

private fun computeBounds(series: List<ChartSeriesData>): ChartBounds? {
    var min = Float.MAX_VALUE
    var max = -Float.MAX_VALUE
    series.forEach { serie ->
        serie.values.forEach { value ->
            if (value != null && value.isFinite()) {
                min = min(min, value)
                max = max(max, value)
            }
        }
    }
    if (min > max) return null
    if (abs(max - min) < FLOAT_EPSILON) {
        // Serie plana: abrimos un margen artificial para no dividir por cero.
        val margin = max(abs(max), 1f) * 0.02f
        return ChartBounds(min = min - margin, max = max + margin)
    }
    return ChartBounds(min = min, max = max)
}

private fun normalize(values: List<Float?>, bounds: ChartBounds?): List<Float?> {
    if (bounds == null) return values.map { null }
    val range = bounds.max - bounds.min
    return values.map { value ->
        value?.takeIf { it.isFinite() }?.let { (it - bounds.min) / range }
    }
}

/** Posición X normalizada del índice en 0..1. */
private fun xFraction(index: Int, pointCount: Int): Float =
    if (pointCount <= 1) 0.5f else index.toFloat() / (pointCount - 1).toFloat()

/**
 * Índice más cercano a una X de pantalla. Se redondea en lugar de interpolar: el usuario
 * quiere el día exacto, no un valor inventado entre dos días.
 */
private fun indexAt(x: Float, width: Float, pointCount: Int, padding: Float): Int? {
    if (pointCount <= 0 || width <= 0f) return null
    val plotWidth = (width - padding * 2f).coerceAtLeast(1f)
    val fraction = ((x - padding) / plotWidth).coerceIn(0f, 1f)
    if (pointCount == 1) return 0
    return (fraction * (pointCount - 1)).roundToInt().coerceIn(0, pointCount - 1)
}

private fun plotPaddingPx(): Float = 14f

private const val FLOAT_EPSILON = 0.0001f
