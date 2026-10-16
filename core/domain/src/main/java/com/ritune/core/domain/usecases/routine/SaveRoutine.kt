package com.ritune.core.domain.usecases.routine

import com.ritune.core.domain.models.routine.SaveResult
import com.ritune.core.domain.repositories.RoutineRepository
import com.ritune.core.models.IdGenerator
import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId
import com.ritune.core.models.Step
import com.ritune.core.models.StepId
import javax.inject.Inject

class SaveRoutine @Inject constructor(
    private val repository: RoutineRepository,
    private val ids: IdGenerator,
) {
    suspend operator fun invoke(input: RoutineInput): SaveResult {
        val issues = RoutineValidator.validate(input)
        if (issues.isNotEmpty()) return SaveResult.Invalid(issues)

        val existing = input.id?.let { repository.get(it) }
        val routine = Routine(
            id = input.id ?: RoutineId(ids.newId()),
            name = input.name.trim(),
            steps = input.steps.map { step ->
                Step(
                    id = step.id ?: StepId(ids.newId()),
                    title = step.title.trim(),
                    duration = step.duration,
                    instruction = step.instruction?.trim()?.ifEmpty { null },
                    cues = step.cues,
                )
            },
            anchorId = input.anchorId,
            builtIn = existing?.builtIn ?: false,
        )
        repository.save(routine)
        return SaveResult.Saved(routine)
    }
}