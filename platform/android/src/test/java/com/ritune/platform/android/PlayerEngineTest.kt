package com.ritune.platform.android

import com.ritune.core.models.RunOutcome
import com.ritune.core.models.player.PlayerEffect
import com.ritune.core.models.player.PlayerState
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aStep
import com.ritune.core.testing.id.SequentialIdGenerator
import com.ritune.core.testing.time.VirtualTimekeeper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerEngineTest {

    private val routine = aRoutine(
        steps = listOf(
            aStep(id = "a", duration = 10.seconds),
            aStep(id = "b", duration = 20.seconds),
        ),
    )

    private fun TestScope.newEngine(): Pair<PlayerEngine, List<PlayerEffect>> {
        val engine = PlayerEngine(
            timekeeper = VirtualTimekeeper(testScheduler),
            ids = SequentialIdGenerator("run"),
            scope = backgroundScope,
        )
        val effects = mutableListOf<PlayerEffect>()
        backgroundScope.launch { engine.effects.collect { effects += it } }
        return engine to effects
    }

    @Test
    fun start_whenIdle_runsAndRejectsSecondStart() = runTest {
        val (engine, _) = newEngine()

        assertTrue(engine.start(routine))
        assertFalse(engine.start(routine))

        val running = assertIs<PlayerState.Running>(engine.state.value)
        assertEquals("run-1", running.session.runId.value)
    }

    @Test
    fun steps_advanceOnlyWhenTheirTimeElapses() = runTest {
        val (engine, _) = newEngine()
        engine.start(routine)

        advanceTimeBy(9_999)
        runCurrent()
        assertEquals(0, assertIs<PlayerState.Running>(engine.state.value).session.stepIndex)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, assertIs<PlayerState.Running>(engine.state.value).session.stepIndex)
    }

    @Test
    fun routine_runsToCompletionAndReportsSummary() = runTest {
        val (engine, effects) = newEngine()
        engine.start(routine)

        advanceTimeBy(30.seconds)
        runCurrent()

        val finished = assertIs<PlayerState.Finished>(engine.state.value)
        assertEquals(RunOutcome.COMPLETED, finished.summary.outcome)
        assertEquals(2, finished.summary.completedSteps)
        assertEquals(PlayerEffect.RunFinished(finished.summary), effects.last())
    }

    @Test
    fun countdown_ticksThreeTwoOneBeforeStepEnds() = runTest {
        val (engine, effects) = newEngine()
        engine.start(routine)

        advanceTimeBy(10.seconds)
        runCurrent()

        val ticks = effects.filterIsInstance<PlayerEffect.CountdownTick>().map { it.secondsLeft }
        assertEquals(listOf(3, 2, 1), ticks)
    }

    @Test
    fun pause_freezesStepUntilResumed() = runTest {
        val (engine, _) = newEngine()
        engine.start(routine)
        advanceTimeBy(4.seconds)

        engine.pause()
        advanceTimeBy(1.hours)
        runCurrent()
        assertEquals(6.seconds, assertIs<PlayerState.Paused>(engine.state.value).remaining)

        engine.resume()
        advanceTimeBy(6.seconds)
        runCurrent()
        assertEquals(1, assertIs<PlayerState.Running>(engine.state.value).session.stepIndex)
    }
}
