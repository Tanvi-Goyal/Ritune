package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.domain.repositories.AnchorEventRepository
import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.models.Timekeeper
import com.ritune.core.models.coach.CoachTool
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolParameter
import com.ritune.core.models.coach.ToolParameterType
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

class ProposeLogCorrectionTool @Inject constructor(
    private val observeAnchors: ObserveAnchors,
    private val events: AnchorEventRepository,
    private val timekeeper: Timekeeper,
) : WriteTool {

    override val definition = DEFINITION

    override suspend fun propose(call: ToolCall): Proposal {
        val eventId = call.stringArgument(LOG_ID)
        val time = call.timeArgument(TIME)

        val now = timekeeper.now().wall
        val zone = timekeeper.timeZone()
        val from = now.toLocalDateTime(zone).date
            .minus(AnchorEventsTool.MAX_DAYS - 1, DateTimeUnit.DAY)
            .atStartOfDayIn(zone)
        val event = events.observeBetween(from, now).first().firstOrNull { it.id.value == eventId }
            ?: throw ToolArgumentException(
                "Unknown $LOG_ID. Call get_anchor_events for the valid ids."
            )
        val anchor = observeAnchors().first().first { it.id == event.anchorId }

        val logged = event.occurredAt.toLocalDateTime(
            UtcOffset(seconds = event.utcOffsetSeconds).asTimeZone()
        )
        if (time == logged.time) throw ToolArgumentException("That log is already at $time.")
        if (logged.date.atTime(time).toInstant(zone) > now) {
            throw ToolArgumentException("$time on ${logged.date} is in the future.")
        }
        return Proposal.LogCorrection(
            original = event,
            anchorName = anchor.name,
            date = logged.date,
            from = logged.time,
            to = time,
        )
    }

    companion object {
        private const val LOG_ID = "log_id"
        private const val TIME = "time"

        val DEFINITION = CoachTool(
            name = "propose_log_correction",
            description = "Proposes moving one logged anchor to a different time on the same day, " +
                    "for a log the user says is wrong. The user sees the change and taps Apply or " +
                    "Decline; the result says which.",
            parameters = listOf(
                ToolParameter(
                    name = LOG_ID,
                    description = "The log's id from get_anchor_events.",
                    type = ToolParameterType.STRING,
                    required = true,
                ),
                ToolParameter(
                    name = TIME,
                    description = "The correct time as a 24-hour time, for example 07:10.",
                    type = ToolParameterType.STRING,
                    required = true,
                ),
            ),
        )
    }
}
