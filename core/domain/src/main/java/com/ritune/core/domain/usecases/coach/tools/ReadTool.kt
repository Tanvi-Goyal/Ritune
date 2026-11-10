package com.ritune.core.domain.usecases.coach.tools

import com.ritune.core.models.coach.CoachTool
import com.ritune.core.models.coach.ToolCall
import kotlinx.serialization.json.Json
import kotlin.time.Duration

/** A tool that only reads the user's data. Returns the JSON the model reads. */
interface ReadTool {
    val definition: CoachTool
    suspend fun run(call: ToolCall): String
}

/** A bad argument from the model. The executor sends the message back so the model can retry. */
class ToolArgumentException(message: String) : Exception(message)

internal val toolJson = Json { explicitNulls = false }

internal fun ToolCall.intArgument(name: String, range: IntRange, default: Int): Int {
    val raw = arguments[name] ?: return default
    val value = raw.toDoubleOrNull()?.toInt()
    if (value == null || value !in range) {
        throw ToolArgumentException("$name must be ${range.first} to ${range.last}")
    }
    return value
}

internal fun ToolCall.stringArgument(name: String): String =
    arguments[name]?.takeIf { it.isNotBlank() } ?: throw ToolArgumentException("$name is required")

internal fun Duration.toToolText(): String {
    val total = inWholeSeconds
    val minutes = total / 60
    val seconds = total % 60
    return when {
        minutes == 0L -> "$seconds s"
        seconds == 0L -> "$minutes min"
        else -> "$minutes min $seconds s"
    }
}
