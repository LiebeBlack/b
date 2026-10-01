package com.liebeblack.divtrack.presentation.settings

import com.liebeblack.divtrack.domain.model.RateSource
import com.liebeblack.divtrack.domain.model.ThemeMode
import com.liebeblack.divtrack.domain.usecase.EnsureSyncScheduledUseCase
import com.liebeblack.divtrack.domain.usecase.ObserveSettingsUseCase
import com.liebeblack.divtrack.domain.usecase.UpdateSettingsUseCase
import com.liebeblack.divtrack.presentation.fake.FakeSettingsRepository
import com.liebeblack.divtrack.presentation.fake.FakeSyncScheduler
import com.liebeblack.divtrack.presentation.rule.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Ajustes: cada cambio se persiste y, cuando toca, reprograma el trabajo periódico. Ese
 * último detalle es el que evita que la preferencia y lo que hace WorkManager se separen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository()
    private val scheduler = FakeSyncScheduler()

    @Test
    fun `el estado refleja las preferencias guardadas`() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)

        val state = viewModel.state.value
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(RateSource.OFICIAL, state.defaultSource)
        assertEquals(false, state.igtfEnabled)
        assertEquals(true, state.autoSyncEnabled)
    }

    @Test
    fun `cambiar el tema persiste el modo elegido`() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onIntent(SettingsIntent.SelectTheme(ThemeMode.DARK))
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, settingsRepository.settings.value.themeMode)
        assertEquals(ThemeMode.DARK, viewModel.state.value.themeMode)
    }

    @Test
    fun `cambiar la frecuencia guarda el intervalo y reprograma el trabajo`() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onIntent(SettingsIntent.SetSyncInterval(60))
        advanceUntilIdle()

        assertEquals(60, settingsRepository.settings.value.syncIntervalMinutes)
        assertEquals(60, scheduler.scheduledIntervalMinutes)
    }

    @Test
    fun `apagar la sincronizacion automatica cancela el trabajo periodico`() = runTest {
        val viewModel = createViewModel()
        collectState(viewModel)

        viewModel.onIntent(SettingsIntent.SetAutoSync(false))
        advanceUntilIdle()

        assertEquals(false, settingsRepository.settings.value.autoSyncEnabled)
        assertEquals(1, scheduler.cancelCalls)
        assertEquals(null, scheduler.scheduledIntervalMinutes)
    }

    private fun TestScope.createViewModel() = SettingsViewModel(
        observeSettings = ObserveSettingsUseCase(settingsRepository),
        updateSettings = UpdateSettingsUseCase(settingsRepository),
        ensureSyncScheduled = EnsureSyncScheduledUseCase(settingsRepository, scheduler),
    )

    private fun TestScope.collectState(viewModel: SettingsViewModel) {
        launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        advanceUntilIdle()
    }
}
