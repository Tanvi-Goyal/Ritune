package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.models.anchor.CorrectResult
import com.ritune.core.domain.models.routine.SaveResult
import com.ritune.core.domain.usecases.anchor.CorrectAnchorTime
import com.ritune.core.domain.usecases.anchor.UpdateAnchorTarget
import com.ritune.core.domain.usecases.coach.tools.toInput
import com.ritune.core.domain.usecases.routine.SaveRoutine
import com.ritune.core.models.coach.AppliedChange
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ProposalOutcome
import javax.inject.Inject

class ApplyCoachProposal @Inject constructor(
    private val updateAnchorTarget: UpdateAnchorTarget,
    private val saveRoutine: SaveRoutine,
    private val correctAnchorTime: CorrectAnchorTime,
) {
    suspend operator fun invoke(proposal: Proposal): ProposalOutcome = when (proposal) {
        is Proposal.AnchorTarget -> {
            updateAnchorTarget(proposal.anchorId, proposal.to)
            ProposalOutcome.Applied(AppliedChange.TargetChanged(proposal))
        }

        is Proposal.NewRoutine -> when (val result = saveRoutine(proposal.toInput())) {
            is SaveResult.Saved -> ProposalOutcome.Applied(
                AppliedChange.RoutineCreated(
                    proposal,
                    result.routine.id
                )
            )

            is SaveResult.Invalid -> ProposalOutcome.NotApplied
        }

        is Proposal.LogCorrection -> {
            val original = proposal.original
            when (
                correctAnchorTime(
                    original.id,
                    original.anchorId,
                    proposal.date,
                    proposal.to
                )
            ) {
                is CorrectResult.Corrected -> ProposalOutcome.Applied(
                    AppliedChange.LogCorrected(
                        proposal
                    )
                )

                CorrectResult.InFuture -> ProposalOutcome.NotApplied
            }
        }
    }
}
