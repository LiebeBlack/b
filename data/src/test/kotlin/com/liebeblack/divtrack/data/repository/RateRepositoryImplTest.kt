package com.liebeblack.divtrack.data.repository

import com.liebeblack.divtrack.core.common.error.DataError
import com.liebeblack.divtrack.core.common.logging.NoOpLogger
import com.liebeblack.divtrack.core.common.result.Result
import com.liebeblack.divtrack.core.common.utils.SourceKeys
import com.liebeblack.divtrack.core.network.error.NetworkErrorMapper
import com.liebeblack.divtrack.core.network.model.RemoteRate
import com.liebeblack.divtrack.core.network.model.RemoteHistoryPoint
import com.liebeblack.divtrack.data.fake.FakeConnectivityObserver
import com.liebeblack.divtrack.data.fake.FakeRateLocalDataSource
import com.liebeblack.divtrack.data.fake.FakeRateProvider
import com.liebeblack.divtrack.data.fake.FakeTimeProvider
import com.liebeblack.divtrack.data.fake.FakeUserPreferencesDataSource
import com.liebeblack.divtrack.data.provider.ProviderRegistry
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.SyncSummary
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Estas pruebas fijan las cuatro promesas del patrón Online-First que sostiene toda la app:
 *
 *  1. La red nunca es la fuente de verdad: escribe en Room y Room re-emite a la UI.
 *  2. Un fallo de red no destruye los datos que el usuario ya tenía.
 *  3. La resolución es por tasa, no por proveedor: que una API caiga no deja la app vacía.
 *  4. Sin conectividad se falla al instante: ni timeout de 10 s ni peticiones inútiles.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RateRepositoryImplTest {

    private val localDataSource = FakeRateLocalDataSource()
    private val preferences = FakeUserPreferencesDataSource()
    private val clock = FakeTimeProvider(today = LocalDate.of(2026, 10, 1))
    private val connectivity = FakeConnectivityObserver()

    private fun repository(vararg providers: FakeRateProvider) = RateRepositoryImpl(
        providerRegistry = ProviderRegistry(
            providers = providers.toSet(),
            errorMapper = NetworkErrorMapper(),
            logger = NoOpLogger,
        ),
        localDataSource = localDataSource,
        preferencesDataSource = preferences,
        timeProvider = clock,
        logger = NoOpLogger,
        connectivityObserver = connectivity,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `completa las dos tasas aunque un proveedor falle`() = runTest {
        val repository = repository(
            FakeRateProvider(
                id = "DolarAPI",
                priority = 0,
                rates = listOf(rate(SourceKeys.OFICIAL, 859.06)),
                failing = true,
            ),
            FakeRateProvider(
                id = "Yadio",
                priority = 10,
                rates = listOf(rate(SourceKeys.PARALELO, 954.55)),
            ),
        )

        val result = repository.refreshRates()

        assertTrue(result is Result.Success)
        val summary = (result as Result.Success<SyncSummary>).data
        assertEquals(listOf(RateSource.OFICIAL, RateSource.PARALELO), summary.updatedSources)
        assertEquals(1, summary.failures.size)
        assertTrue(summary.isPartial)
        assertEquals(2, localDataSource.currentRates.value.size)
    }

    @Test
    fun `si todo falla devuelve error y no toca los datos guardados`() = runTest {
        val repository = repository(
            FakeRateProvider(id = "DolarAPI", priority = 0, failing = true),
            FakeRateProvider(id = "Yadio", priority = 10, failing = true),
        )
        // Primero una sincronización correcta para dejar caché...
        val healthy = repository(
            FakeRateProvider(
                id = "DolarAPI",
                priority = 0,
                rates = listOf(rate(SourceKeys.OFICIAL, 859.06)),
            ),
        )
        healthy.refreshRates()
        val before = localDataSource.currentRates.value

        val result = repository.refreshRates()

        assertTrue(result is Result.Error)
        assertEquals(before, localDataSource.currentRates.value)
        assertEquals(1, localDataSource.upsertCurrentRatesCalls)
    }

    @Test
    fun `sin conectividad falla al instante y no molesta a los proveedores`() = runTest {
        connectivity.isOnline = false
        val provider = FakeRateProvider(
            id = "DolarAPI",
            priority = 0,
            rates = listOf(rate(SourceKeys.OFICIAL, 859.06)),
        )

        val result = repository(provider).refreshRates()

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is DataError.Network)
        assertEquals(0, provider.fetchLatestCalls)
    }

    @Test
    fun `observeRates emite desde Room en orden de presentacion`() = runTest {
        val repository = repository(
            FakeRateProvider(
                id = "DolarAPI",
                priority = 0,
                rates = listOf(
                    rate(SourceKeys.PARALELO, 954.55),
                    rate(SourceKeys.OFICIAL, 859.06),
                ),
            ),
        )
        repository.refreshRates()

        val rates = repository.observeRates().first()

        assertEquals(listOf(RateSource.OFICIAL, RateSource.PARALELO), rates.map { it.source })
        assertEquals(859.06, rates.first().value, DELTA)
        assertEquals("DolarAPI", rates.first().providerId)
    }

    @Test
    fun `cada sincronizacion deja el cierre provisional de hoy en el historico`() = runTest {
        val repository = repository(
            FakeRateProvider(
                id = "DolarAPI",
                priority = 0,
                rates = listOf(rate(SourceKeys.OFICIAL, 859.06)),
            ),
        )

        repository.refreshRates()

        val epochDay = clock.today().toEpochDay()
        val point = localDataSource.history.value.single()
        assertEquals(SourceKeys.OFICIAL, point.source)
        assertEquals(epochDay, point.epochDay)
        assertEquals(859.06, point.value, DELTA)
    }

    @Test
    fun `la flecha de tendencia usa el cierre anterior guardado`() = runTest {
        localDataSource.upsertHistory(
            listOf(historyPoint(SourceKeys.OFICIAL, epochDay = clock.today().toEpochDay() - 1, value = 850.0)),
        )
        val repository = repository(
            FakeRateProvider(
                id = "DolarAPI",
                priority = 0,
                rates = listOf(rate(SourceKeys.OFICIAL, 859.06)),
            ),
        )

        repository.refreshRates()

        val current = localDataSource.currentRates.value.single()
        assertEquals(850.0, current.previousClose!!, DELTA)
        assertEquals(9.06, 859.06 - current.previousClose!!, DELTA)
    }

    @Test
    fun `el historico respeta el ttl de 24 horas y solo importa cuando se fuerza`() = runTest {
        preferences.setLastHistorySyncAt(clock.nowMillis)
        val repository = repository(
            FakeRateProvider(
                id = "DolarAPI",
                priority = 0,
                rates = emptyList(),
                history = listOf(historyPoint(SourceKeys.OFICIAL, epochDay = 20_000L, value = 700.0)),
            ),
        )

        val skipped = repository.syncHistory(force = false)
        assertEquals(0, (skipped as Result.Success).data)
        assertEquals(0, localDataSource.upsertHistoryCalls)

        val imported = repository.syncHistory(force = true)
        assertEquals(1, (imported as Result.Success).data)
        assertEquals(1, localDataSource.history.value.size)
    }

    private fun rate(sourceKey: String, value: Double) = RemoteRate(
        sourceKey = sourceKey,
        value = value,
        updatedAtMillis = null,
    )

    private fun historyPoint(sourceKey: String, epochDay: Long, value: Double) =
        RemoteHistoryPoint(sourceKey = sourceKey, epochDay = epochDay, value = value)

    private companion object {
        const val DELTA = 0.000001
    }
}
