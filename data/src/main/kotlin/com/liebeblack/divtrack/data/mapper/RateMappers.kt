package com.liebeblack.divtrack.data.mapper

import com.liebeblack.divtrack.core.database.entity.CurrentRateEntity
import com.liebeblack.divtrack.core.database.entity.RateHistoryEntity
import com.liebeblack.divtrack.core.network.model.RemoteHistoryPoint
import com.liebeblack.divtrack.core.network.model.RemoteRate
import com.liebeblack.divtrack.domain.model.ExchangeRate
import com.liebeblack.divtrack.domain.model.RateHistoryPoint
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

/** Red -> Room, resolviendo el cierre anterior fuera del mapeo (requiere consulta local). */
internal fun RemoteRate.toEntity(
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

/** Fila vigente -> fila de histórico del día (cierre provisional que mejora con cada sync). */
internal fun CurrentRateEntity.toHistoryEntity(epochDay: Long): RateHistoryEntity = RateHistoryEntity(
    source = source,
    epochDay = epochDay,
    value = value,
    providerId = providerId,
)

internal fun RemoteHistoryPoint.toEntity(providerId: String): RateHistoryEntity = RateHistoryEntity(
    source = sourceKey,
    epochDay = epochDay,
    value = value,
    providerId = providerId,
)

internal fun RateHistoryEntity.toDomain(): RateHistoryPoint = RateHistoryPoint(
    source = RateSource.fromKey(source) ?: RateSource.OFICIAL,
    epochDay = epochDay,
    value = value,
)

