package com.ritune.core.domain.usecases.routine

import com.ritune.core.domain.usecases.routine.RoutineIssue.NAME_BLANK
import com.ritune.core.domain.usecases.routine.RoutineIssue.NAME_TOO_LONG
import com.ritune.core.domain.usecases.routine.RoutineIssue.NO_STEPS
import com.ritune.core.domain.usecases.routine.RoutineIssue.STEP_TITLE_BLANK
import com.ritune.core.domain.usecases.routine.RoutineIssue.STEP_TOO_LONG
import com.ritune.core.domain.usecases.routine.RoutineIssue.STEP_TOO_SHORT
import com.ritune.core.domain.usecases.routine.RoutineIssue.TOO_MANY_STEPS
import com.ritune.core.domain.usecases.routine.RoutineIssue.TOTAL_TOO_LONG
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RoutineValidatorTest {

    private fun step(title: String = "Stretch", duration: Duration = 1.minutes) =
        StepInput(id = null, title = title, duration = duration, instruction = null, cues = emptyList())

    private fun input(name: String = "Morning", steps: List<StepInput> = listOf(step())) =
        RoutineInput(id = null, name = name, anchorId = null, steps = steps)

    private fun issues(input: RoutineInput) = RoutineValidator.validate(input)

    @Test
    fun validate_validRoutine_hasNoIssues() {
        assertEquals(emptySet(), issues(input()))
    }

    @Test
    fun validate_blankName_isNameBlank() {
        assertEquals(setOf(NAME_BLANK), issues(input(name = "   ")))
    }

    @Test
    fun validate_nameLength_allowsFortyTrimmedCharacters() {
        assertEquals(emptySet(), issues(input(name = " ${"a".repeat(40)} ")))
        assertEquals(setOf(NAME_TOO_LONG), issues(input(name = "a".repeat(41))))
    }

    @Test
    fun validate_noSteps_isNoSteps() {
        assertEquals(setOf(NO_STEPS), issues(input(steps = emptyList())))
    }

    @Test
    fun validate_stepCount_allowsTwenty() {
        val short = step(duration = 5.seconds)

        assertEquals(emptySet(), issues(input(steps = List(20) { short })))
        assertEquals(setOf(TOO_MANY_STEPS), issues(input(steps = List(21) { short })))
    }

    @Test
    fun validate_blankStepTitle_isStepTitleBlank() {
        assertEquals(setOf(STEP_TITLE_BLANK), issues(input(steps = listOf(step(title = " ")))))
    }

    @Test
    fun validate_stepDuration_allowsFiveSecondsToSixtyMinutes() {
        val bounds = listOf(step(duration = 5.seconds), step(duration = 60.minutes))

        assertEquals(emptySet(), issues(input(steps = bounds)))
        assertEquals(
            setOf(STEP_TOO_SHORT),
            issues(input(steps = listOf(step(duration = 5.seconds - 1.milliseconds)))),
        )
        assertEquals(
            setOf(STEP_TOO_LONG),
            issues(input(steps = listOf(step(duration = 60.minutes + 1.seconds)))),
        )
    }

    @Test
    fun validate_totalDuration_allowsThreeHours() {
        val hour = step(duration = 60.minutes)

        assertEquals(emptySet(), issues(input(steps = List(3) { hour })))
        assertEquals(
            setOf(TOTAL_TOO_LONG),
            issues(input(steps = List(3) { hour } + step(duration = 5.seconds))),
        )
    }

    @Test
    fun validate_severalProblems_reportsEach() {
        val result = issues(input(name = "", steps = listOf(step(title = ""), step(duration = 1.seconds))))

        assertEquals(setOf(NAME_BLANK, STEP_TITLE_BLANK, STEP_TOO_SHORT), result)
    }
}
