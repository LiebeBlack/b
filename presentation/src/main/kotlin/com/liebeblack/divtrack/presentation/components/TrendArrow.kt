package com.liebeblack.divtrack.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liebeblack.divtrack.domain.model.TrendDirection
import com.liebeblack.divtrack.presentation.common.contentDescriptionRes
import com.liebeblack.divtrack.presentation.theme.DivTrackThemeTokens

/**
 * Flecha direccional verde/roja dibujada a mano.
 *
 * Se dibuja en vez de usar un icono por dos motivos: cero dependencias de recursos y cero
 * coste de fuente de iconos (una `ImageVector` mide y compone más que un `Path` de tres puntos).
 *
 * Como el dibujo no dice nada por sí solo, lleva su significado en la descripción de
 * accesibilidad: TalkBack lee "sube respecto al cierre anterior", no un triángulo mudo.
 */
@Composable
fun TrendArrow(
    direction: TrendDirection,
    modifier: Modifier = Modifier,
    arrowSize: Dp = 12.dp,
) {
    val colors = DivTrackThemeTokens.colors
    val color = when (direction) {
        TrendDirection.UP -> colors.trendUp
        TrendDirection.DOWN -> colors.trendDown
        TrendDirection.FLAT -> colors.trendFlat
    }
    val description = stringResource(direction.contentDescriptionRes())

    Canvas(
        modifier = modifier
            .size(arrowSize)
            .semantics { contentDescription = description },
    ) {
        val width = size.width
        val height = size.height

        when (direction) {
            TrendDirection.UP -> {
                val path = Path().apply {
                    moveTo(width / 2f, 0f)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(path, color)
            }

            TrendDirection.DOWN -> {
                val path = Path().apply {
                    moveTo(width / 2f, height)
                    lineTo(width, 0f)
                    lineTo(0f, 0f)
                    close()
                }
                drawPath(path, color)
            }

            TrendDirection.FLAT -> {
                drawLine(
                    color = color,
                    start = Offset(0f, height / 2f),
                    end = Offset(width, height / 2f),
                    strokeWidth = height / 3f,
                )
            }
        }
    }
}
