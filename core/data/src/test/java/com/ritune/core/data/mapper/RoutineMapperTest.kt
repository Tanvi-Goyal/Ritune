package com.ritune.core.data.mapper

import com.ritune.core.database.entities.CueType
import com.ritune.core.database.entities.RoutineEntity
import com.ritune.core.database.entities.StepCueEntity
import com.ritune.core.database.entities.StepEntity
import com.ritune.core.database.relations.RoutineWithSteps
import com.ritune.core.database.relations.StepWithCues
import com.ritune.core.models.Cue
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.HapticPattern
import com.ritune.core.models.SoundCue
import com.ritune.core.models.StepId
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aStep
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class RoutineMapperTest {

    private val routine = aRoutine(
        anchorId = DefaultCatalog.wake.id,
        builtIn = true,
        steps = listOf(
            aStep(
                id = "stretch",
                instruction = "Slowly",
                cues = listOf(
                    Cue.Haptic(HapticPattern.STEP_CHANGE),
                    Cue.Sound(SoundCue.SOFT_BELL),
                    Cue.Speech("Stretch"),
                ),
            ),
            aStep(id = "breathe", title = "Breathe", duration = 90.seconds),
        ),
    )

    private val routineEntity = RoutineEntity("routine-1", "Test routine", null, 0, builtIn = false)

    private fun step(position: Int, cues: List<StepCueEntity> = emptyList()) = StepWithCues(
        StepEntity(routineId = "routine-1", stepId = "step-$position", position = position,
            title = "Step $position", durationMs = 60_000, instruction = null),
        cues,
    )

    private fun cue(position: Int, type: String, value: String) =
        StepCueEntity(stepRowId = 0, position = position, type = type, value = value)

    @Test
    fun toDraftsAndBack_keepsEveryField() {
        val drafts = routine.toDrafts()
        val stored = RoutineWithSteps(
            routine.toEntity(sortOrder = 2),
            drafts.map { StepWithCues(it.step, it.cues) },
        )

        assertEquals(listOf(0, 1), drafts.map { it.step.position })
        assertEquals(routine, stored.toRoutine())
    }

    @Test
    fun toEntity_overridesBuiltInAndSetsSortOrder() {
        val entity = routine.toEntity(sortOrder = 3, builtIn = true)

        assertEquals(RoutineEntity("routine-1", "Test routine", "wake", 3, builtIn = true), entity)
    }

    @Test
    fun toRoutine_ordersStepsAndCuesByPosition() {
        val stored = RoutineWithSteps(
            routineEntity,
            listOf(
                step(1),
                step(0, cues = listOf(cue(1, CueType.SPEECH, "Second"), cue(0, CueType.SPEECH, "First"))),
            ),
        )

        val mapped = stored.toRoutine()

        assertEquals(listOf(StepId("step-0"), StepId("step-1")), mapped.steps.map { it.id })
        assertEquals(listOf(Cue.Speech("First"), Cue.Speech("Second")), mapped.steps[0].cues)
    }

    @Test
    fun toRoutine_unknownCue_isDropped() {
        val cues = listOf(
            cue(0, "LIGHT", "BLINK"),
            cue(1, CueType.HAPTIC, "RUMBLE"),
            cue(2, CueType.SOUND, "GONG"),
            cue(3, CueType.SOUND, SoundCue.CHIME.name),
        )

        val mapped = RoutineWithSteps(routineEntity, listOf(step(0, cues))).toRoutine()

        assertEquals(listOf(Cue.Sound(SoundCue.CHIME)), mapped.steps.single().cues)
    }
}
