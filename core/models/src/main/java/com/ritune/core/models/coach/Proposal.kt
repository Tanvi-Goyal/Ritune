package com.ritune.core.models.coach

import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.AnchorId
import com.ritune.core.models.RoutineId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

sealed interface Proposal {
    data class AnchorTarget(
        val anchorId: AnchorId,
        val anchorName: String,
        val from: LocalTime?,
        val to: LocalTime,
    ) : Proposal

    data class NewRoutine(
        val name: String,
        val anchorId: AnchorId?,
        val anchorName: String?,
        val steps: List<ProposedStep>,
    ) : Proposal {
        val totalDuration: Duration
            get() = steps.fold(Duration.ZERO) { total, step -> total + step.duration }
    }

    data class LogCorrection(
        val original: AnchorEvent,
        val anchorName: String,
        val date: LocalDate,
        val from: LocalTime,
        val to: LocalTime,
    ) : Proposal
}

data class ProposedStep(val title: String, val duration: Duration)

enum class Decision { APPLY, DECLINE }

sealed interface ProposalOutcome {
    data class Applied(val change: AppliedChange) : ProposalOutcome
    data object Declined : ProposalOutcome
    data object NotApplied : ProposalOutcome
}

sealed interface AppliedChange {
    val proposal: Proposal

    data class TargetChanged(override val proposal: Proposal.AnchorTarget) : AppliedChange
    data class RoutineCreated(
        override val proposal: Proposal.NewRoutine,
        val routineId: RoutineId,
    ) : AppliedChange

    data class LogCorrected(override val proposal: Proposal.LogCorrection) : AppliedChange
}
