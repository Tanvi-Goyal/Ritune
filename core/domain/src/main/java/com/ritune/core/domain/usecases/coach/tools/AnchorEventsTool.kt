package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.repositories.AnchorEventRepository
import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.models.Timekeeper
import com.ritune.core.models.coach.CoachTool
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolParameter
import com.ritune.core.models.coach.ToolParameterType
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class AnchorEventsTool @Inject constructor(
    private val observeAnchors: ObserveAnchors,
    private val events: AnchorEventRepository,
    private val timekeeper: Timekeeper,
) : ReadTool {

    override val definition = DEFINITION

    override suspend fun run(call: ToolCall): String {
        val anchorId = call.stringArgument(ANCHOR_ID)
        val days = call.intArgument(DAYS, 1..MAX_DAYS, default = 7)
        val anchor = observeAnchors().first().firstOrNull { it.id.value == anchorId }
            ?: throw ToolArgumentException("Unknown $ANCHOR_ID. Call get_anchors for the valid ids.")

        val now = timekeeper.now().wall
        val zone = timekeeper.timeZone()
        val from = now.toLocalDateTime(zone).date.minus(days - 1, DateTimeUnit.DAY).atStartOfDayIn(zone)
        val logs = events.observeBetween(from, now).first().filter { it.anchorId == anchor.id }
        return toolJson.encodeToString(logs.toResult(anchor, days))
    }

    companion object {
        const val MAX_DAYS = 60
        private const val ANCHOR_ID = "anchor_id"
        private const val DAYS = "days"

        val DEFINITION = CoachTool(
            name = "get_anchor_events",
            description = "Every time the user logged one anchor over the last few days, oldest " +
                "first, as the local date and time it was logged.",
            parameters = listOf(
                ToolParameter(
                    name = ANCHOR_ID,
                    description = "The anchor's id from get_anchors.",
                    type = ToolParameterType.STRING,
                    required = true,
                ),
                ToolParameter(
                    name = DAYS,
                    description = "How many days back to read, including today, 1 to $MAX_DAYS. " +
                        "Defaults to 7.",
                    type = ToolParameterType.INTEGER,
                    required = false,
                ),
            ),
        )
    }
}
