package com.ritune.core.data.mapper

import com.ritune.core.database.entities.CueType
import com.ritune.core.database.entities.RoutineEntity
import com.ritune.core.database.entities.StepCueEntity
import com.ritune.core.database.entities.StepEntity
import com.ritune.core.database.relations.RoutineWithSteps
import com.ritune.core.database.relations.StepDraft
import com.ritune.core.database.relations.StepWithCues
import com.ritune.core.models.AnchorId
import com.ritune.core.models.Cue
import com.ritune.core.models.HapticPattern
import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId
import com.ritune.core.models.SoundCue
import com.ritune.core.models.Step
import com.ritune.core.models.StepId
import kotlin.time.Duration.Companion.milliseconds


internal fun RoutineWithSteps.toRoutine() = Routine(
    id = RoutineId(routine.id),
    name = routine.name,
    steps = steps.sortedBy { it.step.position }.map { it.toStep() },
    anchorId = routine.anchorId?.let(::AnchorId),
    builtIn = routine.builtIn,
)

fun StepWithCues.toStep() = Step(
    id = StepId(step.stepId),
    title = step.title,
    duration = step.durationMs.milliseconds,
    instruction = step.instruction,
    cues = cues.sortedBy { it.position }.mapNotNull { it.toCueOrNull() },
)

private fun StepCueEntity.toCueOrNull(): Cue? = when (type) {
    CueType.HAPTIC -> enumOrNull<HapticPattern>(value)?.let { Cue.Haptic(it) }
    CueType.SOUND -> enumOrNull<SoundCue>(value)?.let { Cue.Sound(it) }
    CueType.SPEECH -> Cue.Speech(value)
    else -> null
}

internal fun Routine.toEntity(
    sortOrder: Int,
    builtIn: Boolean = this.builtIn
) = RoutineEntity(
    id = id.value,
    name = name,
    anchorId = anchorId?.value,
    sortOrder = sortOrder,
    builtIn = builtIn,
)

internal fun Routine.toDrafts(): List<StepDraft> = steps.mapIndexed { index, step ->
    StepDraft(
        step = StepEntity(
            routineId = id.value,
            stepId = step.id.value,
            position = index,
            title = step.title,
            durationMs = step.duration.inWholeMilliseconds,
            instruction = step.instruction,
        ),
        cues = step.cues.mapIndexed { cueIndex, cue -> cue.toEntity(cueIndex) },
    )
}

private fun Cue.toEntity(position: Int): StepCueEntity = when (this) {
    is Cue.Haptic -> StepCueEntity(
        stepRowId = 0,
        position = position,
        type = CueType.HAPTIC,
        value = pattern.name
    )

    is Cue.Sound -> StepCueEntity(
        stepRowId = 0,
        position = position,
        type = CueType.SOUND,
        value = sound.name
    )

    is Cue.Speech -> StepCueEntity(
        stepRowId = 0,
        position = position,
        type = CueType.SPEECH,
        value = text
    )
}

private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? =
    enumValues<E>().firstOrNull { it.name == name }
