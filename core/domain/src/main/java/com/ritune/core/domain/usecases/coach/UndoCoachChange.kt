package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.repositories.AnchorEventRepository
import com.ritune.core.domain.usecases.anchor.UpdateAnchorTarget
import com.ritune.core.domain.usecases.routine.DeleteRoutine
import com.ritune.core.models.coach.AppliedChange
import javax.inject.Inject

class UndoCoachChange @Inject constructor(
    private val updateAnchorTarget: UpdateAnchorTarget,
    private val deleteRoutine: DeleteRoutine,
    private val events: AnchorEventRepository,
) {
    suspend operator fun invoke(change: AppliedChange) {
        when (change) {
            is AppliedChange.TargetChanged ->
                updateAnchorTarget(
                    change.proposal.anchorId,
                    change.proposal.from
                )

            is AppliedChange.RoutineCreated -> deleteRoutine(change.routineId)
            is AppliedChange.LogCorrected -> events.replace(
                change.proposal.original.id,
                change.proposal.original
            )
        }
    }
}
