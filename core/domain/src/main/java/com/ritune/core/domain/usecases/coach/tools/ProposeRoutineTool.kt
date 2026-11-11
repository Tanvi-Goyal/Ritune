package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.domain.usecases.routine.RoutineInput
import com.ritune.core.domain.usecases.routine.RoutineIssue
import com.ritune.core.domain.usecases.routine.RoutineValidator
import com.ritune.core.domain.usecases.routine.StepInput
import com.ritune.core.models.coach.CoachTool
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ProposedStep
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolParameter
import com.ritune.core.models.coach.ToolParameterType
import com.ritune.core.models.defaultStepCues
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class ProposeRoutineTool @Inject constructor(
    private val observeAnchors: ObserveAnchors,
) : WriteTool {

    override val definition = DEFINITION

    override suspend fun propose(call: ToolCall): Proposal {
        val name = call.stringArgument(NAME)
        val anchor = call.arguments[ANCHOR_ID]?.takeIf { it.isNotBlank() }?.let { id ->
            observeAnchors().first().firstOrNull { it.id.value == id }
                ?: throw ToolArgumentException(
                    "Unknown $ANCHOR_ID. Call get_anchors for the valid ids."
                )
        }

        val steps = runCatching {
            toolJson.decodeFromString<List<StepArgument>>(call.stringArgument(STEPS))
        }.getOrElse {
            throw ToolArgumentException(
                "$STEPS must be a list of {title, duration_seconds}"
            )
        }

        val proposal = Proposal.NewRoutine(
            name = name.trim(),
            anchorId = anchor?.id,
            anchorName = anchor?.name,
            steps = steps.map {
                ProposedStep(
                    it.title.trim(),
                    it.durationSeconds.toLong().seconds
                )
            },
        )
        val issues = RoutineValidator.validate(proposal.toInput())
        if (issues.isNotEmpty()) {
            throw ToolArgumentException(
                "The routine breaks these rules: " +
                        issues.joinToString { it.rule() }
            )
        }
        return proposal
    }

    @Serializable
    private data class StepArgument(
        val title: String,
        // Double, because the model may send 300 as 300.0.
        @SerialName(DURATION_SECONDS) val durationSeconds: Double,
    )

    companion object {
        private const val NAME = "name"
        private const val ANCHOR_ID = "anchor_id"
        private const val STEPS = "steps"
        private const val DURATION_SECONDS = "duration_seconds"

        val DEFINITION = CoachTool(
            name = "propose_routine",
            description = "Proposes a new routine. The user sees it and taps Apply or Decline; " +
                    "the result says which. It can't change an existing routine.",
            parameters = listOf(
                ToolParameter(
                    name = NAME,
                    description = "Up to ${RoutineValidator.MAX_NAME_LENGTH} characters.",
                    type = ToolParameterType.STRING,
                    required = true,
                ),
                ToolParameter(
                    name = ANCHOR_ID,
                    description = "The id from get_anchors of the anchor this routine belongs to, if any.",
                    type = ToolParameterType.STRING,
                    required = false,
                ),
                ToolParameter(
                    name = STEPS,
                    description = "1 to ${RoutineValidator.MAX_STEPS} steps in order. The whole " +
                            "routine is at most ${RoutineValidator.MAX_TOTAL.inWholeMinutes} minutes.",
                    type = ToolParameterType.OBJECT_LIST,
                    required = true,
                    fields = listOf(
                        ToolParameter(
                            name = "title",
                            description = "What to do, in a few words.",
                            type = ToolParameterType.STRING,
                            required = true,
                        ),
                        ToolParameter(
                            name = DURATION_SECONDS,
                            description = "${RoutineValidator.MIN_STEP.inWholeSeconds} to " +
                                    "${RoutineValidator.MAX_STEP.inWholeSeconds}.",
                            type = ToolParameterType.INTEGER,
                            required = true,
                        ),
                    ),
                ),
            ),
        )
    }
}

internal fun Proposal.NewRoutine.toInput() = RoutineInput(
    id = null,
    name = name,
    anchorId = anchorId,
    steps = steps.map {
        StepInput(
            id = null,
            title = it.title,
            duration = it.duration,
            instruction = null,
            cues = defaultStepCues(it.title),
        )
    },
)

private fun RoutineIssue.rule() = when (this) {
    RoutineIssue.NAME_BLANK -> "the name is required"
    RoutineIssue.NAME_TOO_LONG ->
        "the name is at most ${RoutineValidator.MAX_NAME_LENGTH} characters"

    RoutineIssue.NO_STEPS -> "at least one step"
    RoutineIssue.TOO_MANY_STEPS -> "at most ${RoutineValidator.MAX_STEPS} steps"
    RoutineIssue.STEP_TITLE_BLANK -> "every step needs a title"
    RoutineIssue.STEP_TOO_SHORT ->
        "each step is at least ${RoutineValidator.MIN_STEP.inWholeSeconds} seconds"

    RoutineIssue.STEP_TOO_LONG ->
        "each step is at most ${RoutineValidator.MAX_STEP.inWholeSeconds} seconds"

    RoutineIssue.TOTAL_TOO_LONG ->
        "the whole routine is at most ${RoutineValidator.MAX_TOTAL.inWholeMinutes} minutes"
}
