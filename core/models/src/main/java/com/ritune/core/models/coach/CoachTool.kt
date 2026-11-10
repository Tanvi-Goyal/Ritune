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
)

enum class ToolParameterType { INTEGER, STRING }

/** The model's request to run [name]. Argument values arrive as their JSON text. */
data class ToolCall(
    val id: String?,
    val name: String,
    val arguments: Map<String, String> = emptyMap(),
)

/** What the app sends back for [call]: a JSON object computed by code. */
data class ToolResult(
    val call: ToolCall,
    val json: String,
)

/** The fixed part of every request: the system instructions and the tools on offer. */
data class CoachSetup(
    val instructions: String,
    val tools: List<CoachTool>,
)
