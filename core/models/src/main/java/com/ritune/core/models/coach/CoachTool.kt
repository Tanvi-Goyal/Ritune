package com.ritune.core.models.coach

/** A tool the model may ask the app to run, described without any provider's types. */
data class CoachTool(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter> = emptyList(),
)

data class ToolParameter(
    val name: String,
    val description: String,
    val type: ToolParameterType,
    val required: Boolean,
    val fields: List<ToolParameter> = emptyList(),
)

enum class ToolParameterType { INTEGER, STRING, OBJECT_LIST }

data class ToolCall(
    val id: String?,
    val name: String,
    val arguments: Map<String, String> = emptyMap(),
)

data class ToolResult(
    val call: ToolCall,
    val json: String,
)

data class CoachSetup(
    val instructions: String,
    val tools: List<CoachTool>,
)
