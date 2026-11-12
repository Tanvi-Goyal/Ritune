package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.models.coach.CoachTool
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ToolCall
import kotlinx.datetime.LocalTime

interface WriteTool {
    val definition: CoachTool
    suspend fun propose(call: ToolCall): Proposal
}

internal fun ToolCall.timeArgument(name: String): LocalTime {
    val raw = stringArgument(name)
    return runCatching { LocalTime.parse(raw) }.getOrNull()
        ?.takeIf { it.second == 0 && it.nanosecond == 0 }
        ?: throw ToolArgumentException("$name must be a 24-hour time like 07:30")
}
