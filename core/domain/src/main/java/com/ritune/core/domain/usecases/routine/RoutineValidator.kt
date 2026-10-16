package com.ritune.core.domain.usecases.routine

import com.ritune.core.models.AnchorId
import com.ritune.core.models.Cue
import com.ritune.core.models.RoutineId
import com.ritune.core.models.StepId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object RoutineValidator {
    const val MAX_NAME_LENGTH = 40
    const val MAX_STEPS = 20
    val MIN_STEP: Duration = 5.seconds
    val MAX_STEP: Duration = 60.minutes
    val MAX_TOTAL: Duration = 3.hours

    fun validate(input: RoutineInput): Set<RoutineIssue> = buildSet {
        val name = input.name.trim()
        if (name.isEmpty()) add(RoutineIssue.NAME_BLANK)
        if (name.length > MAX_NAME_LENGTH) add(RoutineIssue.NAME_TOO_LONG)
        if (input.steps.isEmpty()) add(RoutineIssue.NO_STEPS)
        if (input.steps.size > MAX_STEPS) add(RoutineIssue.TOO_MANY_STEPS)
        input.steps.forEach { step ->
            if (step.title.isBlank()) add(RoutineIssue.STEP_TITLE_BLANK)
            if (step.duration < MIN_STEP) add(RoutineIssue.STEP_TOO_SHORT)
            if (step.duration > MAX_STEP) add(RoutineIssue.STEP_TOO_LONG)
        }
        if (input.steps.fold(Duration.ZERO) { total, step ->
                total + step.duration
            } > MAX_TOTAL
        ) {
            add(RoutineIssue.TOTAL_TOO_LONG)
        }
    }
}

/** What the editor holds while the user types. May be invalid, unlike [Routine]. Null ids mean "new". */
data class RoutineInput(
    val id: RoutineId?,
    val name: String,
    val anchorId: AnchorId?,
    val steps: List<StepInput>,
)

data class StepInput(
    val id: StepId?,
    val title: String,
    val duration: Duration,
    val instruction: String?,
    val cues: List<Cue>,
)

enum class RoutineIssue {
    NAME_BLANK,
    NAME_TOO_LONG,
    NO_STEPS,
    TOO_MANY_STEPS,
    STEP_TITLE_BLANK,
    STEP_TOO_SHORT,
    STEP_TOO_LONG,
    TOTAL_TOO_LONG,
}