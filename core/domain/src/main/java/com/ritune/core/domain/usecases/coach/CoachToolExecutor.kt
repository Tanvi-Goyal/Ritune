package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.usecases.coach.tools.AnchorEventsTool
import com.ritune.core.domain.usecases.coach.tools.AnchorsTool
import com.ritune.core.domain.usecases.coach.tools.RoutinesTool
import com.ritune.core.domain.usecases.coach.tools.RunHistoryTool
import com.ritune.core.domain.usecases.coach.tools.ToolArgumentException
import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Runs the tools the model asks for.
 * Bad requests return an error the model can read and recover from, instead of failing the turn.
 */
class CoachToolExecutor @Inject constructor(
    weekRhythm: WeekRhythmTool,
    anchors: AnchorsTool,
    anchorEvents: AnchorEventsTool,
    runHistory: RunHistoryTool,
    routines: RoutinesTool,
) {
    private val tools = listOf(weekRhythm, anchors, anchorEvents, runHistory, routines)
        .associateBy { it.definition.name }

    suspend fun execute(call: ToolCall): ToolResult {
        val tool = tools[call.name] ?: return ToolResult(call, errorJson("Unknown tool: ${call.name}"))
        val result = try {
            tool.run(call)
        } catch (e: ToolArgumentException) {
            errorJson(e.message.orEmpty())
        }
        return ToolResult(call, result)
    }

    private fun errorJson(message: String) = Json.encodeToString(ErrorResult(message))
}

@Serializable
private data class ErrorResult(val error: String)
