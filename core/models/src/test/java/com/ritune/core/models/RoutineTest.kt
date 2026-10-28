package com.ritune.core.models

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class RoutineTest {

    private fun step(id: String, duration: Duration = 1.minutes) =
        Step(StepId(id), "Step $id", duration)

    private val routine = Routine(
        RoutineId("routine"),
        "Morning",
        listOf(step("a", 30.seconds), step("b", 2.minutes)),
    )

    @Test
    fun create_withoutSteps_fails() {
        assertFailsWith<IllegalArgumentException> {
            Routine(RoutineId("empty"), "Empty", emptyList())
        }
    }

    @Test
    fun createStep_withZeroDuration_fails() {
        assertFailsWith<IllegalArgumentException> { step("zero", Duration.ZERO) }
    }

    @Test
    fun totalDuration_sumsEveryStep() {
        assertEquals(150.seconds, routine.totalDuration)
    }

    @Test
    fun session_followsStepIndex() {
        val first = Session(RunId("run"), routine, stepIndex = 0, startedAt = Instant.DISTANT_PAST)
        val last = first.copy(stepIndex = 1)

        assertEquals(StepId("a"), first.currentStep.id)
        assertEquals(false, first.isLastStep)
        assertEquals(StepId("b"), last.currentStep.id)
        assertEquals(true, last.isLastStep)
    }

    @Test
    fun defaultStepCues_speaksTrimmedTitleWithHapticAndBell() {
        assertEquals(
            listOf(
                Cue.Haptic(HapticPattern.STEP_CHANGE),
                Cue.Speech("Stretch"),
                Cue.Sound(SoundCue.SOFT_BELL),
            ),
            defaultStepCues("  Stretch "),
        )
    }
}
