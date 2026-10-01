package com.liebeblack.divtrack.data.repository

import com.liebeblack.divtrack.core.common.error.DataError
import com.liebeblack.divtrack.core.common.logging.Logger
import com.liebeblack.divtrack.core.common.result.Result
import com.liebeblack.divtrack.core.common.time.TimeProvider
import com.liebeblack.divtrack.core.common.utils.AppConstants
import com.liebeblack.divtrack.core.database.datasource.RateLocalDataSource
import com.liebeblack.divtrack.core.datastore.UserPreferencesDataSource
import com.liebeblack.divtrack.core.network.monitor.ConnectivityObserver
import com.liebeblack.divtrack.data.di.IoDispatcher
import com.liebeblack.divtrack.data.mapper.toDailyCloseEntity
import com.liebeblack.divtrack.data.mapper.toDomain
import com.liebeblack.divtrack.data.mapper.toEntity
import com.liebeblack.divtrack.data.provider.ProviderRegistry
import com.liebeblack.divtrack.domain.model.ExchangeRate
import com.liebeblack.divtrack.domain.model.ProviderFailure
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.SyncSummary
import com.liebeblack.divtrack.domain.repository.RateRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Implementación Online-First.
 *
 * Reglas que se cumplen aquí (y que son el corazón del proyecto):
 *
 * 1. **Room es la única fuente de verdad para la UI.** Todas las lecturas son `Flow` de
 *    Room: la pantalla se actualiza sola cuando la red escribe y sigue funcionando offline.
 * 2. **Un fallo de red no toca los datos.** Si los proveedores fallan, no se escribe nada
 *    y se devuelve [Result.Error]; la UI muestra el aviso con lo que ya tenía.
 * 3. **Single-flight.** Un [Mutex] evita que el refresco manual, el arranque y WorkManager
 *    disparen tres sincronizaciones simultáneas.
 * 4. **El cierre de cada día se persiste.** Cada sync escribe la fila de hoy en el cierre
 *    diario, de modo que mañana existe un "cierre anterior" real para la flecha de tendencia
 *    incluso si la app estuvo cerrada.
 */
@Singleton
class RateRepositoryImpl @Inject constructor(
    private val providerRegistry: ProviderRegistry,
    private val localDataSource: RateLocalDataSource,
    private val preferencesDataSource: UserPreferencesDataSource,
    private val timeProvider: TimeProvider,
    private val logger: Logger,
    private val connectivityObserver: ConnectivityObserver,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RateRepository {

    private val syncMutex = Mutex()

    override fun observeRates(): Flow<List<ExchangeRate>> =
        localDataSource.observeCurrentRates()
            .map { entities ->
                entities
                    .map { entity -> entity.toDomain() }
                    .sortedBy { rate -> rate.source.displayOrder }
            }
            .flowOn(ioDispatcher)

    override suspend fun refreshRates(): Result<SyncSummary> = withContext(ioDispatcher) {
        syncMutex.withLock { performRatesRefresh() }
    }

    private suspend fun performRatesRefresh(): Result<SyncSummary> {
        // Fallo rápido: sin conectividad no se espera al timeout para decir "sin conexión".
        // El usuario ve el aviso al instante y se ahorra el trabajo de red. Es una pista,
        // no un requisito: `ConnectivityObserver` solo responde "false" cuando de verdad no
        // hay ninguna red activa, así que un falso negativo no puede dejar la app muda.
        if (!connectivityObserver.isOnline()) {
            logger.warn(TAG, "Sincronización omitida: el sistema no reporta ninguna red activa")
            return Result.Error(DataError.Network(message = "Sin conexión a internet"))
        }

        val outcome = providerRegistry.fetchLatest()

        if (outcome.rates.isEmpty()) {
            val error = mostInformativeFailure(outcome.failures)
            logger.warn(TAG, "Sincronización sin datos: ${error.message ?: error::class.simpleName}")
            return Result.Error(error)
        }

        val epochDay = timeProvider.today().toEpochDay()

        val entities = outcome.rates.mapNotNull { resolved ->
            val source = RateSource.fromKey(resolved.rate.sourceKey) ?: return@mapNotNull null
            resolved.rate.toEntity(
                providerId = resolved.providerId,
                previousClose = resolvePreviousClose(
                    source = source,
                    epochDay = epochDay,
                    currentValue = resolved.rate.value,
                ),
                fetchedAtMillis = outcome.fetchedAtMillis,
            )
        }

        if (entities.isEmpty()) {
            return Result.Error(DataError.Parse(message = "Los proveedores no devolvieron tasas utilizables"))
        }

        // Escritura única: Room re-emite a las tres pantallas automáticamente.
        localDataSource.upsertCurrentRates(entities)
        localDataSource.upsertDailyCloses(entities.map { entity -> entity.toDailyCloseEntity(epochDay) })
        preferencesDataSource.setLastSyncAt(outcome.fetchedAtMillis)

        // Retención del cierre diario: la tabla crece dos filas al día. La poda viaja con la
        // sincronización (y no en un trabajo aparte) para que no exista ninguna ruta que
        // escriba cierres sin aplicarle nunca el límite de antigüedad.
        pruneOldDailyCloses()

        if (outcome.failures.isNotEmpty()) {
            logger.warn(TAG, "Sincronización parcial: ${entities.size} tasas, ${outcome.failures.size} fallos")
        }

        return Result.Success(
            SyncSummary(
                updatedSources = entities.mapNotNull { entity -> RateSource.fromKey(entity.source) },
                providerIds = outcome.providerIds,
                failures = outcome.failures,
                fetchedAtMillis = outcome.fetchedAtMillis,
            ),
        )
    }

    /**
     * Cierre anterior en este orden:
     * 1. Última fila del cierre diario con día menor a hoy (el caso normal).
     * 2. El valor vigente previo, para que la primera sincronización ya muestre tendencia.
     */
    private suspend fun resolvePreviousClose(
        source: RateSource,
        epochDay: Long,
        currentValue: Double,
    ): Double? {
        localDataSource.previousClose(source.key, epochDay)?.let { return it.value }
        return localDataSource.currentRate(source.key)?.value?.takeIf { it != currentValue }
    }

    private suspend fun pruneOldDailyCloses() {
        val keepFromEpochDay = timeProvider.today()
            .minusYears(AppConstants.DAILY_CLOSE_KEEP_YEARS)
            .toEpochDay()
        localDataSource.pruneDailyCloses(keepFromEpochDay)
    }

    /**
     * Elige el error que más explica al usuario.
     *
     * Con varios proveedores es normal acabar con un `Network` genérico (el DNS no resolvió)
     * y un `Http` concreto (el proveedor devolvió 404). Enseñar el genérico escondería la
     * causa real, y el usuario leería "sin conexión" cuando sí tenía conexión.
     */
    private fun mostInformativeFailure(failures: List<ProviderFailure>): DataError =
        failures.firstOrNull { failure -> failure.error !is DataError.Network }?.error
            ?: failures.firstOrNull()?.error
            ?: DataError.EmptyCache(message = "Los proveedores de tasas no respondieron")

    private companion object {
        const val TAG = "RateRepository"
    }
}
