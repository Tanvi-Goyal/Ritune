package com.ritune.core.testing.data

import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.AnchorId
import com.ritune.core.models.Cue
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.EventId
import com.ritune.core.models.EventSource
import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId
import com.ritune.core.models.RunId
import com.ritune.core.models.RunOutcome
import com.ritune.core.models.RunSummary
import com.ritune.core.models.Session
import com.ritune.core.models.Step
import com.ritune.core.models.StepId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

val testInstant: Instant = Instant.parse("2026-01-05T08:00:00Z")

fun aStep(
    id: String = "step-1",
    title: String = "Stretch",
    duration: Duration = 1.minutes,
    instruction: String? = null,
    cues: List<Cue> = emptyList(),
) = Step(StepId(id), title, duration, instruction, cues)

fun aRoutine(
    id: String = "routine-1",
    name: String = "Test routine",
    steps: List<Step> = listOf(aStep()),
    anchorId: AnchorId? = null,
    builtIn: Boolean = false,
) = Routine(RoutineId(id), name, steps, anchorId, builtIn)

fun anAnchorEvent(
    id: String = "event-1",
    anchorId: AnchorId = DefaultCatalog.wake.id,
    occurredAt: Instant = testInstant,
    utcOffsetSeconds: Int = 0,
    source: EventSource = EventSource.MANUAL,
) = AnchorEvent(EventId(id), anchorId, occurredAt, utcOffsetSeconds, source)

fun aSession(
    routine: Routine = aRoutine(),
    runId: String = "run-1",
    stepIndex: Int = 0,
    startedAt: Instant = testInstant,
) = Session(RunId(runId), routine, stepIndex, startedAt)

fun aRunSummary(
    routine: Routine = aRoutine(),
    runId: String = "run-1",
    startedAt: Instant = testInstant,
    finishedAt: Instant = startedAt + routine.totalDuration,
    outcome: RunOutcome = RunOutcome.COMPLETED,
    completedSteps: Int = routine.steps.size,
    skippedSteps: Int = 0,
) = RunSummary(
    runId = RunId(runId),
    routineId = routine.id,
    routineName = routine.name,
    startedAt = startedAt,
    finishedAt = finishedAt,
    outcome = outcome,
    completedSteps = completedSteps,
    skippedSteps = skippedSteps,
    totalSteps = routine.steps.size,
)
