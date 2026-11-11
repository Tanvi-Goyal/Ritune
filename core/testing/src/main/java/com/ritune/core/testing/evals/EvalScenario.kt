package com.ritune.core.testing.evals

import com.ritune.core.models.AnchorId
import com.ritune.core.models.coach.Decision
import com.ritune.core.models.coach.Proposal
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

/** A question (or a short conversation) on a fixed [world], with what a good answer must do. */
data class EvalScenario(
    val id: String,
    val category: EvalCategory,
    val world: EvalWorld,
    val turns: List<EvalTurn>,
)

enum class EvalCategory { RHYTHM, RUNS, UNANSWERABLE, MEDICAL, INJECTION, WRITE, MULTI_TURN }

data class EvalTurn(
    val question: String,
    val expect: Expectation = Expectation(),
    /** What the user taps if the coach proposes a change. */
    val decision: Decision = Decision.DECLINE,
    /** Undo the applied change before the next turn, as the Undo button does. */
    val undoAfter: Boolean = false,
)

data class Expectation(
    /** Tools that must be called during the turn. */
    val tools: Set<String> = emptySet(),
    val forbiddenTools: Set<String> = emptySet(),
    val noTools: Boolean = false,
    /** Null means the turn must not propose anything. */
    val proposal: ExpectedProposal? = null,
    val disclaimer: Boolean = false,
    /** Every number in the reply must appear in a tool result, the question or the prompt. */
    val grounded: Boolean = true,
    /** For turns that ask for a suggestion: a new target time may appear, other numbers may not. */
    val allowNewTimes: Boolean = false,
    val mentionsTime: LocalTime? = null,
    val mustNotMatch: List<Regex> = emptyList(),
)

data class ExpectedProposal(val label: String, val matches: (Proposal) -> Boolean) {
    companion object {
        val any = ExpectedProposal("any proposal") { true }

        fun target(anchor: AnchorId, to: LocalTime) = ExpectedProposal("${anchor.value} → $to") {
            it is Proposal.AnchorTarget && it.anchorId == anchor && it.to == to
        }

        fun routine(anchor: AnchorId?, maxTotal: Duration) =
            ExpectedProposal("routine on ${anchor?.value}, ≤ $maxTotal") {
                it is Proposal.NewRoutine && it.anchorId == anchor && it.totalDuration <= maxTotal
            }

        fun logFix(anchor: AnchorId, to: LocalTime) = ExpectedProposal("log ${anchor.value} → $to") {
            it is Proposal.LogCorrection && it.original.anchorId == anchor && it.to == to
        }
    }
}
