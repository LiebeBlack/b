package com.liebeblack.divtrack.presentation.dashboard

import com.liebeblack.divtrack.core.common.error.DataError
import com.liebeblack.divtrack.core.common.result.Result
import com.liebeblack.divtrack.domain.model.ExchangeRate
import com.liebeblack.divtrack.domain.model.ProviderFailure
import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.SyncSummary
import com.liebeblack.divtrack.domain.usecase.CalculateSpreadUseCase
import com.liebeblack.divtrack.domain.usecase.ObserveRatesUseCase
import com.liebeblack.divtrack.domain.usecase.SyncRatesUseCase
import com.liebeblack.divtrack.presentation.R
import com.liebeblack.divtrack.presentation.common.UiText
import com.liebeblack.divtrack.presentation.fake.FakeRateRepository
import com.liebeblack.divtrack.presentation.rule.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * El dashboard es donde el pliego pone dos requisitos explícitos:
 *  1. Si la API responde, la UI refleja los datos (y la brecha se calcula sola).
 *  2. Si falla, se conservan los datos de Room y se emite un evento one-shot para el snackbar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val rateRepository = FakeRateRepository()

    @Test
    fun `las tarjetas y la brecha se derivan de las tasas guardadas`() = runTest {
        rateRepository.rates.value = listOf(
            rate(RateSource.OFICIAL, 859.06, previousClose = 858.0),
            rate(RateSource.PARALELO, 954.55, previousClose = 960.0),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.rates.size)
        assertEquals("859,06 Bs.", state.rates.first { it.source == RateSource.OFICIAL }.valueText)
        assertEquals("954,55 Bs.", state.rates.first { it.source == RateSource.PARALELO }.valueText)
        assertEquals("+11,12 %", state.spreadPercentText)
        assertEquals("95,49 Bs.", state.spreadAbsoluteText)
    }

    @Test
    fun `si la red falla conserva el cache y emite el aviso de sin conexion`() = runTest {
        rateRepository.rates.value = listOf(
            rate(RateSource.OFICIAL, 859.06, previousClose = null),
            rate(RateSource.PARALELO, 954.55, previousClose = null),
        )
        rateRepository.refreshResult = Result.Error(DataError.Network())

        val viewModel = createViewModel()
        val effects = mutableListOf<DashboardEffect>()
        collectEffects(viewModel, effects)

        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.rates.size)
        assertTrue(viewModel.state.value.isOffline)
        assertEquals(
            DashboardEffect.ShowMessage(UiText.Res(R.string.msg_offline_showing_cache)),
            effects.single(),
        )
    }

    @Test
    fun `una sincronizacion parcial avisa sin perder datos`() = runTest {
        rateRepository.rates.value = listOf(rate(RateSource.OFICIAL, 859.06, previousClose = null))
        rateRepository.refreshResult = Result.Success(
            SyncSummary(
                updatedSources = listOf(RateSource.OFICIAL),
                providerIds = listOf("DolarAPI"),
                failures = listOf(ProviderFailure("Yadio", DataError.Timeout())),
                fetchedAtMillis = 0L,
            ),
        )

        val viewModel = createViewModel()
        val effects = mutableListOf<DashboardEffect>()
        collectEffects(viewModel, effects)

        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.rates.size)
        assertEquals(
            DashboardEffect.ShowMessage(UiText.Res(R.string.msg_partial_update)),
            effects.single(),
        )
    }

    private fun TestScope.createViewModel() = DashboardViewModel(
        observeRates = ObserveRatesUseCase(rateRepository),
        syncRates = SyncRatesUseCase(rateRepository),
        calculateSpread = CalculateSpreadUseCase(),
    )

    /**
     * Recolecta los eventos one-shot en una lista propia, con un dispatcher no confinado
     * para que la suscripción exista **antes** de que el `init` del ViewModel emita. Es la
     * forma determinista de testear un `SharedFlow` sin depender de tiempos.
     *
     * Va en `backgroundScope` porque `toList` no completa nunca: si se lanzara en el scope
     * del test, `runTest` esperaría por él hasta agotar su tiempo límite.
     */
    private fun TestScope.collectEffects(
        viewModel: DashboardViewModel,
        destination: MutableList<DashboardEffect>,
    ) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.toList(destination)
        }
    }

    private fun rate(source: RateSource, value: Double, previousClose: Double?) = ExchangeRate(
        source = source,
        value = value,
        previousClose = previousClose,
        providerId = "DolarAPI",
        updatedAtMillis = null,
        fetchedAtMillis = 0L,
    )
}
