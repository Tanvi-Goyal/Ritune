package com.ritune.platform.ai.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.UsageMetadata
import com.google.firebase.ai.type.content
import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.CoachInput
import com.ritune.core.models.coach.CoachTool
import com.ritune.core.models.coach.CoachUsage
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolParameter
import com.ritune.core.models.coach.ToolParameterType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.io.IOException

internal fun CoachTool.toDeclaration() = FunctionDeclaration(
    name,
    description,
    parameters.associate { it.name to it.toSchema() },
    parameters.filterNot { it.required }.map { it.name },
)

private fun ToolParameter.toSchema() = when (type) {
    ToolParameterType.INTEGER -> Schema.integer(description)
    ToolParameterType.STRING -> Schema.string(description)
}

internal fun FunctionCallPart.toToolCall() = ToolCall(
    id = id,
    name = name,
    arguments = args.mapValues { (_, value) ->
        (value as? JsonPrimitive)?.content
            ?: value.toString()
    },
)

internal fun CoachInput.toContent(): Content = when (this) {
    is CoachInput.UserMessage -> content { text(text) }
    is CoachInput.ToolResults -> content(role = TOOL_RESULT_ROLE) {
        results.forEach { result ->
            part(
                FunctionResponsePart(
                    result.call.name,
                    Json.parseToJsonElement(result.json).jsonObject,
                    result.call.id,
                ),
            )
        }
    }
}

internal fun UsageMetadata.toCoachUsage() = CoachUsage(
    inputTokens = promptTokenCount,
    outputTokens = candidatesTokenCount ?: 0,
    thinkingTokens = thoughtsTokenCount ?: 0,
)

internal fun Throwable.toCoachError(): CoachError = when {
    this is QuotaExceededException -> CoachError.QUOTA_EXCEEDED
    this is PromptBlockedException || this is ResponseStoppedException ||
            this is ContentBlockedException -> CoachError.BLOCKED

    this is RequestTimeoutException ||
            causes().any {
                it is IOException || it is FirebaseNetworkException
            } -> CoachError.NETWORK

    else -> CoachError.UNKNOWN
}

private fun Throwable.causes() = generateSequence(this) { it.cause }

// The Developer API rejects the "function" role older docs show; tool results go back as the user.
internal const val TOOL_RESULT_ROLE = "user"
