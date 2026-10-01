package com.liebeblack.divtrack.presentation.dashboard

import androidx.compose.runtime.Immutable
import com.liebeblack.divtrack.core.common.utils.CurrencyFormatters
import com.liebeblack.divtrack.domain.model.ExchangeRate
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.TrendDirection
import com.liebeblack.divtrack.presentation.common.UiText
import java.time.Instant

/**
 * Modelo de una tasa ya formateado para pintar.
 *
 * Todo son primitivas o enums: `@Immutable` + campos estables hacen que Compose pueda
 * saltarse la recomposición de la tarjeta cuando solo cambia la otra tasa.
 */
@Immutable
data class RateUiModel(
    val source: RateSource,
    val valueText: String,
    val deltaText: String?,
    val trend: TrendDirection,
    val providerText: String,
    val updatedAtText: String?,
)

/** Estado completo del dashboard. Es la única fuente que consume la pantalla. */
@Immutable
data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val rates: List<RateUiModel> = emptyList(),
    val spreadPercentText: String? = null,
    val spreadAbsoluteText: String? = null,

    /** La última sincronización falló. */
    val isOffline: Boolean = false,

    /**
     * El fallo fue de conectividad (y no del proveedor). Decide si tiene sentido ofrecer el
     * atajo a los ajustes de red: mandar al usuario a sus ajustes cuando el que falla es el
     * proveedor solo consigue que toque cosas que no arreglan nada.
     */
    val isConnectivityProblem: Boolean = false,

    /** Causa real del último fallo, ya traducida a un recurso de texto. */
    val errorText: UiText? = null,
) {
    val hasData: Boolean get() = rates.isNotEmpty()
}

/** Dominio -> UI. El formateo es-VE se hace una sola vez, aquí, no en cada recomposición. */
internal fun ExchangeRate.toRateUiModel(): RateUiModel = RateUiModel(
    source = source,
    valueText = CurrencyFormatters.bolivars(value),
    deltaText = changePercent?.takeIf { it.isFinite() }?.let { CurrencyFormatters.percent(it) },
    trend = trend,
    providerText = providerId,
    updatedAtText = updatedAtMillis?.let { millis ->
        runCatching { CurrencyFormatters.timestamp(Instant.ofEpochMilli(millis)) }.getOrNull()
    },
)
