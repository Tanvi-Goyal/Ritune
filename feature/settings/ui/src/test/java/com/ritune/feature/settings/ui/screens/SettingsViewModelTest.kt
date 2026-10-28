package com.ritune.feature.settings.ui.screens

import app.cash.turbine.test
import com.ritune.core.domain.usecases.DeleteAllData
import com.ritune.core.domain.usecases.player.ObservePlayerState
import com.ritune.core.domain.usecases.settings.ObserveSettings
import com.ritune.core.domain.usecases.settings.UpdateSettings
import com.ritune.core.models.AppSettings
import com.ritune.core.models.ThemeSetting
import com.ritune.core.models.player.PlayerState
import com.ritune.core.testing.data.aSession
import com.ritune.core.testing.ports.FakeDataResetter
import com.ritune.core.testing.ports.FakePlayerController
import com.ritune.core.testing.repository.FakeSettingsRepository
import com.ritune.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository(
        AppSettings(
            hapticsEnabled = false,
            speechEnabled = false,
            soundEnabled = false,
            countdownEnabled = false,
            theme = ThemeSetting.SYSTEM,
        ),
    )
    private val player = FakePlayerController()
    private val resetter = FakeDataResetter()

    private fun TestScope.newViewModel(): SettingsViewModel {
        val viewModel = SettingsViewModel(
            ObserveSettings(settings),
            UpdateSettings(settings),
            ObservePlayerState(player),
            DeleteAllData(resetter),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        return viewModel
    }

    private fun SettingsViewModel.content() = assertIs<SettingsUiState.Content>(state.value)

    @Test
    fun cueToggles_saveEachSetting() = runTest {
        val viewModel = newViewModel()

        viewModel.onEvent(SettingsEvent.HapticsChanged(true))
        viewModel.onEvent(SettingsEvent.SpeechChanged(true))
        viewModel.onEvent(SettingsEvent.SoundChanged(true))
        viewModel.onEvent(SettingsEvent.CountdownChanged(true))
        runCurrent()

        val saved = settings.settings.value
        assertTrue(saved.hapticsEnabled && saved.speechEnabled && saved.soundEnabled && saved.countdownEnabled)
        val content = viewModel.content()
        assertTrue(content.haptics && content.speech && content.sound && content.countdown)
    }

    @Test
    fun themeSelected_savesThemeAndClosesSheet() = runTest {
        val viewModel = newViewModel()

        viewModel.onEvent(SettingsEvent.ThemeClicked)
        assertTrue(viewModel.content().themeSheetOpen)
        viewModel.onEvent(SettingsEvent.ThemeSelected(ThemeSetting.DARK))
        runCurrent()

        assertEquals(ThemeSetting.DARK, settings.settings.value.theme)
        assertEquals(ThemeSetting.DARK, viewModel.content().theme)
        assertFalse(viewModel.content().themeSheetOpen)
    }

    @Test
    fun dismissTheme_closesSheetWithoutSaving() = runTest {
        val viewModel = newViewModel()

        viewModel.onEvent(SettingsEvent.ThemeClicked)
        viewModel.onEvent(SettingsEvent.DismissTheme)
        runCurrent()

        assertFalse(viewModel.content().themeSheetOpen)
        assertEquals(ThemeSetting.SYSTEM, settings.settings.value.theme)
    }

    @Test
    fun routineRunningOrPaused_marksRoutineActive() = runTest {
        val viewModel = newViewModel()
        assertFalse(viewModel.content().routineActive)

        player.state.value = PlayerState.Running(aSession(), stepEndsAtMono = 10_000)
        assertTrue(viewModel.content().routineActive)

        player.state.value = PlayerState.Paused(aSession(), remaining = 5.seconds)
        assertTrue(viewModel.content().routineActive)
    }

    @Test
    fun requestDelete_routineActive_doesNotConfirm() = runTest {
        player.state.value = PlayerState.Running(aSession(), stepEndsAtMono = 10_000)
        val viewModel = newViewModel()

        viewModel.onEvent(SettingsEvent.RequestDelete)

        assertFalse(viewModel.content().confirmingDelete)
    }

    @Test
    fun requestThenDismissDelete_togglesConfirmation() = runTest {
        val viewModel = newViewModel()

        viewModel.onEvent(SettingsEvent.RequestDelete)
        assertTrue(viewModel.content().confirmingDelete)
        viewModel.onEvent(SettingsEvent.DismissDelete)

        assertFalse(viewModel.content().confirmingDelete)
        assertEquals(0, resetter.resetCount)
    }

    @Test
    fun confirmDelete_success_resetsDataAndReportsDeleted() = runTest {
        val viewModel = newViewModel()
        viewModel.onEvent(SettingsEvent.RequestDelete)

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.ConfirmDelete)
            assertEquals(SettingsEffect.DataDeleted, awaitItem())
        }
        assertEquals(1, resetter.resetCount)
        assertFalse(viewModel.content().confirmingDelete)
        assertFalse(viewModel.content().deleting)
    }

    @Test
    fun confirmDelete_failure_reportsDeleteFailed() = runTest {
        resetter.error = IOException("disk full")
        val viewModel = newViewModel()
        viewModel.onEvent(SettingsEvent.RequestDelete)

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.ConfirmDelete)
            assertEquals(SettingsEffect.DeleteFailed, awaitItem())
        }
        assertFalse(viewModel.content().confirmingDelete)
        assertFalse(viewModel.content().deleting)
    }

    @Test
    fun confirmDelete_whileDeleting_runsOnce() = runTest {
        resetter.gate = CompletableDeferred()
        val viewModel = newViewModel()

        viewModel.onEvent(SettingsEvent.ConfirmDelete)
        runCurrent()
        assertTrue(viewModel.content().deleting)
        viewModel.onEvent(SettingsEvent.ConfirmDelete)
        resetter.gate!!.complete(Unit)
        runCurrent()

        assertEquals(1, resetter.resetCount)
    }

    @Test
    fun confirmDelete_routineActive_doesNothing() = runTest {
        player.state.value = PlayerState.Paused(aSession(), remaining = 5.seconds)
        val viewModel = newViewModel()

        viewModel.onEvent(SettingsEvent.ConfirmDelete)
        runCurrent()

        assertEquals(0, resetter.resetCount)
    }

    @Test
    fun confirmDelete_cancelled_reportsNothing() = runTest {
        resetter.error = CancellationException("cleared")
        val viewModel = newViewModel()

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.ConfirmDelete)
            runCurrent()
            expectNoEvents()
        }
    }
}
