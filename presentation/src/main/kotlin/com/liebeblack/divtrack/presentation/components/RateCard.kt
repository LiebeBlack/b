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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
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
import androidx.compose.ui.text.font.FontWeight
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
    val emphasisColor = if (isHero) accentColor else colors.parallelMuted

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isHero) {
            colors.officialAccent.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            width = if (isHero) 1.5.dp else 1.dp,
            color = if (isHero) {
                colors.officialAccent.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
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
                    .height(if (isHero) 5.dp else 3.dp)
                    .background(if (isHero) emphasisColor else colors.parallelMuted.copy(alpha = 0.55f)),
            )

            Column(
                modifier = Modifier.padding(
                    horizontal = Spacing.lg,
                    vertical = if (isHero) Spacing.lg else Spacing.md,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = if (isHero) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleSmall
                        },
                        color = emphasisColor,
                        maxLines = if (LocalDensity.current.fontScale >= 1.3f) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (isHero) {
                        Surface(
                            color = colors.officialAccent.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = Spacing.sm,
                                    vertical = Spacing.xs,
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            color = colors.officialAccent,
                                            shape = MaterialTheme.shapes.small,
                                        ),
                                )
                                Text(
                                    text = stringResource(R.string.rate_primary_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = emphasisColor,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val fontScale = LocalDensity.current.fontScale
                    Text(
                        text = valueText,
                        style = if (isHero && maxWidth >= 400.dp && fontScale < 1.3f) {
                            MaterialTheme.typography.displayMedium.tabular()
                        } else {
                            MaterialTheme.typography.displaySmall.tabular()
                        },
                        color = emphasisColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    if (deltaText != null) {
                        Surface(
                            color = (if (isHero) trendColor else colors.parallelMuted)
                                .copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = Spacing.sm,
                                    vertical = Spacing.xs,
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                TrendArrow(
                                    direction = trend,
                                    arrowSize = 10.dp,
                                    tint = if (isHero) trendColor else colors.parallelMuted,
                                )
                                Text(
                                    text = deltaText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isHero) trendColor else colors.parallelMuted,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    Text(
                        text = providerText,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isHero) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            colors.parallelMuted
                        },
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (updatedAtText != null || isStale) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = listOfNotNull(
                            updatedAtText,
                            if (isStale) stringResource(R.string.rate_stale_warning) else null,
                        ).joinToString(separator = " · "),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isStale) {
                            MaterialTheme.colorScheme.tertiary
                        } else if (isHero) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            colors.parallelMuted
                        },
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
    }
}
