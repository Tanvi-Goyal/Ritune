package com.ritune.platform.ai.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.TextPart
import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.CoachInput
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolResult
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FirebaseMappersTest {

    @Test
    fun functionCall_becomesToolCallWithPlainArguments() {
        val part = FunctionCallPart(
            name = "get_week_rhythm",
            args = mapOf("week_offset" to JsonPrimitive(1), "label" to JsonPrimitive("wake")),
            id = "call-1",
        )

        assertEquals(
            ToolCall(
                id = "call-1",
                name = "get_week_rhythm",
                arguments = mapOf("week_offset" to "1", "label" to "wake")
            ),
            part.toToolCall(),
        )
    }

    @Test
    fun userMessage_becomesUserTextContent() {
        val content = CoachInput.UserMessage("How was my week?").toContent()

        assertEquals("user", content.role)
        assertEquals("How was my week?", assertIs<TextPart>(content.parts.single()).text)
    }

    @Test
    fun toolResults_becomeFunctionResponsesKeepingTheCallId() {
        val call = ToolCall(id = "call-1", name = "get_week_rhythm")
        val content =
            CoachInput.ToolResults(listOf(ToolResult(call, """{"consistency_percent":80}""")))
                .toContent()

        assertEquals("user", content.role)
        val part = assertIs<FunctionResponsePart>(content.parts.single())
        assertEquals("get_week_rhythm", part.name)
        assertEquals("call-1", part.id)
        assertEquals(JsonPrimitive(80), part.response["consistency_percent"])
    }

    @Test
    fun ioFailureAnywhereInTheCauseChain_isNetwork() {
        assertEquals(
            CoachError.NETWORK, IllegalStateException("wrapped", IOException("offline"))
                .toCoachError()
        )
        assertEquals(CoachError.UNKNOWN, IllegalStateException("bug").toCoachError())
    }

    @Test
    fun signInWithoutNetwork_isNetwork() {
        assertEquals(CoachError.NETWORK, FirebaseNetworkException("offline").toCoachError())
    }
}
