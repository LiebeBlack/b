package com.liebeblack.divtrack.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liebeblack.divtrack.domain.model.TrendDirection
import com.liebeblack.divtrack.presentation.R
import com.liebeblack.divtrack.presentation.theme.DivTrackThemeTokens
import com.liebeblack.divtrack.presentation.theme.Spacing
import com.liebeblack.divtrack.presentation.theme.tabular

/**
 * Tarjeta de una tasa con geometría simétrica: franja de acento superior, todo el
 * contenido centrado y pie apilado que envuelve en vez de cortarse.
 *
 * Por qué cambió el diseño: la barra lateral de color comprimía horizontalmente el pie
 * ("Fuente: X" + "Actualizado Y" competían por el ancho y quedaban cortados o bajados),
 * y el bloque interior quedaba alineado a la izquierda dentro de una tarjeta que la
 * pantalla centra. La franja superior da el mismo código de color sin costo de ancho.
 *
 * [isHero] escala la cifra: el dólar oficial es la referencia de todo (contratos,
 * alquileres, la calculadora por defecto) y se muestra como protagonista
 * (`displayMedium`, 38 sp); el paralelo se muestra un 20 % menor (`displaySmall`,
 * 30 sp) para que la jerarquía se lea de un vistazo.
 *
 * Composable sin estado (primitivas + lambda): Compose puede saltarse la recomposición
 * cuando nada cambia, que es exactamente lo que buscamos en gama baja.
 */
@Composable
fun RateCard(
    title: String,
    valueText: String,
    trend: TrendDirection,
    deltaText: String?,
    providerText: String,
    updatedAtText: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isStale: Boolean = false,
    isHero: Boolean = false,
) {
    val colors = DivTrackThemeTokens.colors
    val trendColor = when (trend) {
        TrendDirection.UP -> colors.trendUp
        TrendDirection.DOWN -> colors.trendDown
        TrendDirection.FLAT -> colors.trendFlat
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Código de color de la fuente como cabecera: se ve con el mismo relieve
            // que la barra lateral pero nunca roba ancho al contenido.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentColor),
            )

            Column(
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                if (deltaText != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TrendArrow(direction = trend, arrowSize = 11.dp)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            text = deltaText,
                            style = MaterialTheme.typography.labelMedium,
                            color = trendColor,
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.xs))
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val fontScale = LocalDensity.current.fontScale
                    Text(
                        text = valueText,
                        style = if (isHero && maxWidth >= 400.dp && fontScale < 1.3f) {
                            MaterialTheme.typography.displayMedium.tabular()
                        } else {
                            MaterialTheme.typography.displaySmall.tabular()
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Pie apilado y centrado: cada dato en su línea. Envuelve en pantallas
                // angostas en lugar de competir por el ancho y quedar cortado.
                Text(
                    text = providerText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                if (updatedAtText != null) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = updatedAtText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isStale) {
                            // La edad del dato es la información: el cierre de ayer se
                            // lee distinto cuando el banco lleva un día sin publicar.
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                    )
                }

                if (isStale) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(R.string.rate_stale_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
