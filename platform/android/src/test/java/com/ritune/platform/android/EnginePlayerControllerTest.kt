package com.ritune.platform.android

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.domain.ports.RunRecorder
import com.ritune.core.domain.ports.StartResult
import com.ritune.core.models.Cue
import com.ritune.core.models.RunOutcome
import com.ritune.core.models.RunSummary
import com.ritune.core.models.Session
import com.ritune.core.models.player.PlayerState
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aStep
import com.ritune.core.testing.id.SequentialIdGenerator
import com.ritune.core.testing.ports.FakeCuePlayer
import com.ritune.core.testing.ports.FakeRunRecorder
import com.ritune.core.testing.time.VirtualTimekeeper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class EnginePlayerControllerTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val cue = FakeCuePlayer()
    private val logger = FakeLogger()

    private val stepCues: List<Cue> = listOf(Cue.Speech("Stretch"))
    private val routine = aRoutine(steps = listOf(aStep(duration = 5.seconds, cues = stepCues)))

    private fun TestScope.newController(recorder: RunRecorder = FakeRunRecorder()) = EnginePlayerController(
        engine = PlayerEngine(VirtualTimekeeper(testScheduler), SequentialIdGenerator("run"), backgroundScope),
        recorder = recorder,
        cuePlayer = cue,
        scope = backgroundScope,
        context = context,
        logger = logger,
    )

    private fun nextStartedService() = shadowOf(context).nextStartedService

    @Test
    fun start_whenIdle_startsServiceAndReturnsStarted() = runTest {
        val controller = newController()

        assertEquals(StartResult.Started, controller.start(routine))

        assertEquals(PlayerService::class.java.name, nextStartedService()?.component?.className)
        assertIs<PlayerState.Running>(controller.state.value)
    }

    @Test
    fun start_whenRunning_returnsAlreadyRunningWithoutNewService() = runTest {
        val controller = newController()
        controller.start(routine)
        nextStartedService()

        assertEquals(StartResult.AlreadyRunning, controller.start(routine))
        assertNull(nextStartedService())
    }

    @Test
    fun run_routesEffectsToCuesAndRecorder() = runTest {
        val recorder = FakeRunRecorder()
        val controller = newController(recorder)

        controller.start(routine)
        runCurrent()

        assertTrue(cue.prepared)
        assertEquals(listOf(routine), recorder.started.map { it.routine })
        assertEquals(listOf(stepCues), cue.played)

        advanceTimeBy(6.seconds)
        runCurrent()

        assertEquals(listOf(3, 2, 1), cue.countdowns)
        assertEquals(RunOutcome.COMPLETED, recorder.finished.single().outcome)
        assertTrue(cue.released)
    }

    @Test
    fun effectHandler_throws_isLoggedAndLaterEffectsStillRun() = runTest {
        val failure = IllegalStateException("history unavailable")
        val failingRecorder = object : RunRecorder {
            override suspend fun onRunStarted(session: Session) = throw failure
            override suspend fun onRunFinished(summary: RunSummary) = Unit
        }
        val controller = newController(failingRecorder)

        controller.start(routine)
        runCurrent()

        assertEquals(listOf<Throwable?>(failure), logger.errors)
        assertEquals(listOf(stepCues), cue.played)
    }

    @Test
    fun commands_driveTheEngine() = runTest {
        val controller = newController()
        controller.start(routine)

        controller.pause()
        assertIs<PlayerState.Paused>(controller.state.value)
        controller.resume()
        assertIs<PlayerState.Running>(controller.state.value)
        controller.skipStep()
        assertEquals(1, assertIs<PlayerState.Finished>(controller.state.value).summary.skippedSteps)
        controller.reset()
        assertEquals(PlayerState.Idle, controller.state.value)
        controller.start(routine)
        controller.stop()
        assertEquals(RunOutcome.STOPPED, assertIs<PlayerState.Finished>(controller.state.value).summary.outcome)
    }
}
