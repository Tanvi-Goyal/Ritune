package com.ritune.coach

import com.ritune.core.domain.usecases.coach.EndCoachConversation
import com.ritune.core.domain.usecases.coach.RunCoachTurn
import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.CoachException
import com.ritune.core.models.coach.CoachUsage
import com.ritune.core.models.coach.ConversationId
import com.ritune.core.models.coach.ModelEvent
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.coach.coachToolExecutor
import com.ritune.core.testing.id.SequentialIdGenerator
import com.ritune.core.testing.ports.FakeCoachGateway
import com.ritune.core.testing.rules.MainDispatcherRule
import com.ritune.core.testing.time.VirtualTimekeeper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CoachDebugViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val gateway = FakeCoachGateway()

    private fun TestScope.newViewModel(): CoachDebugViewModel {
        val timekeeper = VirtualTimekeeper(testScheduler)
        val executor = coachToolExecutor(timekeeper)
        return CoachDebugViewModel(
            RunCoachTurn(gateway, executor),
            EndCoachConversation(gateway),
            SequentialIdGenerator("chat"),
            timekeeper,
        )
    }

    @Test
    fun send_showsTheQuestionAndStreamedAnswer() = runTest {
        gateway.reply(
            ModelEvent.ToolCallRequest(ToolCall("call-1", WeekRhythmTool.DEFINITION.name)),
            ModelEvent.Finished(CoachUsage(10, 2, 0)),
        )
        gateway.reply(ModelEvent.Text("Steady "), ModelEvent.Text("week."), ModelEvent.Finished(CoachUsage(30, 4, 0)))
        val viewModel = newViewModel()

        viewModel.onEvent(CoachDebugEvent.InputChanged("How was my week?"))
        viewModel.onEvent(CoachDebugEvent.Send)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(
            listOf(CoachMessage(fromUser = true, "How was my week?"), CoachMessage(fromUser = false, "Steady week.")),
            state.messages,
        )
        assertEquals("", state.input)
        assertFalse(state.answering)
        assertEquals(listOf(WeekRhythmTool.DEFINITION.name), state.lastTurn?.tools)
        assertEquals(CoachUsage(40, 6, 0), state.lastTurn?.usage)
    }

    @Test
    fun failedTurn_showsTheErrorAndStartsAFreshConversation() = runTest {
        gateway.failure = CoachException(CoachError.QUOTA_EXCEEDED)
        val viewModel = newViewModel()

        viewModel.onEvent(CoachDebugEvent.Ask("hi"))
        advanceUntilIdle()

        assertEquals(CoachError.QUOTA_EXCEEDED, viewModel.state.value.error)
        assertFalse(viewModel.state.value.answering)
        assertEquals(listOf(ConversationId("chat-1")), gateway.closed)
    }

    @Test
    fun newConversation_clearsTheScreenAndForgetsTheOldOne() = runTest {
        gateway.reply(ModelEvent.Text("Hi"), ModelEvent.Finished(CoachUsage()))
        val viewModel = newViewModel()
        viewModel.onEvent(CoachDebugEvent.Ask("hello"))
        advanceUntilIdle()

        viewModel.onEvent(CoachDebugEvent.NewConversation)

        assertEquals(CoachDebugUiState(), viewModel.state.value)
        assertEquals(listOf(ConversationId("chat-1")), gateway.closed)
    }

    @Test
    fun blankMessage_isNotSent() = runTest {
        val viewModel = newViewModel()

        viewModel.onEvent(CoachDebugEvent.Ask("   "))
        advanceUntilIdle()

        assertEquals(emptyList(), gateway.sent)
        assertNull(viewModel.state.value.lastTurn)
    }
}
