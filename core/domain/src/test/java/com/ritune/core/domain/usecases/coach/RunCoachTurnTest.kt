package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.CoachEvent
import com.ritune.core.models.coach.CoachException
import com.ritune.core.models.coach.CoachInput
import com.ritune.core.models.coach.CoachUsage
import com.ritune.core.models.coach.ConversationId
import com.ritune.core.models.coach.ModelEvent
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.coach.coachToolExecutor
import com.ritune.core.testing.ports.FakeCoachGateway
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunCoachTurnTest {

    private val gateway = FakeCoachGateway()
    private val timekeeper = FakeTimekeeper()
    private val executor = coachToolExecutor(timekeeper)
    private val runCoachTurn = RunCoachTurn(gateway, executor)
    private val conversation = ConversationId("c-1")
    private val weekRhythmCall = ToolCall(id = "call-1", name = WeekRhythmTool.DEFINITION.name)

    @Test
    fun answerWithoutTools_streamsTextThenDone() = runTest {
        gateway.reply(ModelEvent.Text("Hi"), ModelEvent.Finished(CoachUsage(2, 3, 0)))

        val events = runCoachTurn(conversation, "hello").toList()

        assertEquals(
            listOf<CoachEvent>(CoachEvent.TextDelta("Hi"), CoachEvent.Done(CoachUsage(2, 3, 0))),
            events,
        )
        assertEquals(listOf<CoachInput>(CoachInput.UserMessage("hello")), gateway.sent)
    }

    @Test
    fun toolRequest_runsTheToolAndSendsItsResultBack() = runTest {
        gateway.reply(
            ModelEvent.ToolCallRequest(weekRhythmCall),
            ModelEvent.Finished(CoachUsage(10, 5, 20)),
        )
        gateway.reply(ModelEvent.Text("Steady week."), ModelEvent.Finished(CoachUsage(30, 4, 0)))

        val events = runCoachTurn(conversation, "How was my week?").toList()

        assertEquals(
            listOf<CoachEvent>(
                CoachEvent.ToolRunning(WeekRhythmTool.DEFINITION.name),
                CoachEvent.TextDelta("Steady week."),
                CoachEvent.Done(CoachUsage(40, 9, 20)),
            ),
            events,
        )
        val sentBack = assertIs<CoachInput.ToolResults>(gateway.sent[1]).results.single()
        assertEquals(weekRhythmCall, sentBack.call)
        assertTrue(sentBack.json.contains("\"week\""))
    }

    @Test
    fun modelKeepsAskingForTools_failsAfterTheCap() = runTest {
        repeat(RunCoachTurn.MAX_TOOL_ROUNDS + 1) {
            gateway.reply(ModelEvent.ToolCallRequest(weekRhythmCall), ModelEvent.Finished(CoachUsage()))
        }

        val events = runCoachTurn(conversation, "loop").toList()

        assertEquals(CoachEvent.Failed(CoachError.TOO_MANY_TOOL_CALLS), events.last())
        assertEquals(RunCoachTurn.MAX_TOOL_ROUNDS + 1, gateway.sent.size)
    }

    @Test
    fun gatewayFailure_failsTheTurnWithItsError() = runTest {
        gateway.failure = CoachException(CoachError.QUOTA_EXCEEDED)

        val events = runCoachTurn(conversation, "hi").toList()

        assertEquals(listOf<CoachEvent>(CoachEvent.Failed(CoachError.QUOTA_EXCEEDED)), events)
    }

    @Test
    fun unexpectedFailure_failsTheTurnAsUnknown() = runTest {
        val events = runCoachTurn(conversation, "no reply scripted").toList()

        assertEquals(listOf<CoachEvent>(CoachEvent.Failed(CoachError.UNKNOWN)), events)
    }
}
