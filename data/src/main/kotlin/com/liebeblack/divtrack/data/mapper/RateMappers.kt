package com.liebeblack.divtrack.data.mapper

import com.liebeblack.divtrack.core.database.entity.CurrentRateEntity
import com.liebeblack.divtrack.core.database.entity.RateHistoryEntity
import com.liebeblack.divtrack.core.network.model.RemoteRate
import com.liebeblack.divtrack.domain.model.ExchangeRate
import com.liebeblack.divtrack.domain.model.RateSource

/** Room -> dominio. */
internal fun CurrentRateEntity.toDomain(): ExchangeRate = ExchangeRate(
    source = RateSource.fromKey(source) ?: RateSource.OFICIAL,
    value = value,
    previousClose = previousClose,
    providerId = providerId,
    updatedAtMillis = updatedAtMillis,
    fetchedAtMillis = fetchedAtMillis,
)

/**
 * Red -> Room. El cierre anterior se resuelve fuera del mapeo (requiere consulta local) y el
 * proveedor entra por parámetro: `RemoteRate` describe el dato, no quién lo publicó.
 */
internal fun RemoteRate.toEntity(
    providerId: String,
    previousClose: Double?,
    fetchedAtMillis: Long,
): CurrentRateEntity = CurrentRateEntity(
    source = sourceKey,
    value = value,
    previousClose = previousClose,
    providerId = providerId,
    updatedAtMillis = updatedAtMillis,
    fetchedAtMillis = fetchedAtMillis,
)

/**
 * Tasa vigente -> cierre del día.
 *
 * Es lo que permite que mañana exista un "cierre anterior" real con el que pintar la flecha
 * de tendencia del panel, incluso si la app estuvo cerrada. Se escribe en cada sincronización
 * y el `@Upsert` por `(source, epoch_day)` hace que repetir el día sea idempotente: la fila
 * del día en curso mejora con cada refresco en lugar de duplicarse.
 *
 * No alimenta ninguna pantalla de histórico: esa sección se eliminó del producto.
 */
internal fun CurrentRateEntity.toDailyCloseEntity(epochDay: Long): RateHistoryEntity =
    RateHistoryEntity(
        source = source,
        epochDay = epochDay,
        value = value,
        providerId = providerId,
    )
