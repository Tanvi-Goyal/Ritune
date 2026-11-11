package com.ritune.core.domain.usecases.coach

import com.ritune.core.models.coach.Proposal

internal object CoachNotes {
    fun withUndone(message: String, undone: List<Proposal>): String {
        if (undone.isEmpty()) return message
        val changes = undone.joinToString("; ") { it.undoneText() }
        return "[App note: since your last reply the user undid: $changes.]\n\n$message"
    }

    private fun Proposal.undoneText() = when (this) {
        is Proposal.AnchorTarget ->
            "the $anchorName target change, so it is ${from ?: "unset"} again"

        is Proposal.NewRoutine ->
            "the new routine \"$name\", so it was deleted"

        is Proposal.LogCorrection ->
            "the $anchorName log fix on $date, so it is $from again"
    }
}
