package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.usecases.rhythm.ObserveCurrentDay
import com.ritune.core.domain.usecases.rhythm.ObserveWeekRhythm
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolResult
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Runs the tools the model asks for. Every number comes from the same use cases the screens
 * use, so the coach and the Rhythm screen always agree.
 * Bad requests return an error the model can read and recover from, instead of failing the turn.
 */
class CoachToolExecutor @Inject constructor(
    private val observeCurrentDay: ObserveCurrentDay,
    private val observeWeekRhythm: ObserveWeekRhythm,
) {
    suspend fun execute(call: ToolCall): ToolResult {
        val result = when (call.name) {
            CoachTools.weekRhythm.name -> weekRhythm(call)
            else -> errorJson("Unknown tool: ${call.name}")
        }
        return ToolResult(call, result)
    }

    private suspend fun weekRhythm(call: ToolCall): String {
        val offset = call.arguments[WEEK_OFFSET]?.let { it.toDoubleOrNull()?.toInt() ?: -1 } ?: 0
        if (offset !in 0..CoachTools.MAX_WEEK_OFFSET) {
            return errorJson("$WEEK_OFFSET must be 0 to ${CoachTools.MAX_WEEK_OFFSET}")
        }
        val weekEnd = observeCurrentDay().first().minus(offset * 7, DateTimeUnit.DAY)
        val rhythm = observeWeekRhythm(weekEnd).first()
        return json.encodeToString(rhythm.toResult(weekEnd))
    }

    private fun errorJson(message: String) = json.encodeToString(ErrorResult(message))

    private companion object {
        const val WEEK_OFFSET = "week_offset"
        val json = Json { explicitNulls = false }
    }
}

@Serializable
private data class ErrorResult(val error: String)
