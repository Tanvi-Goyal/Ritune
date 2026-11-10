package com.ritune.feature.coach.ui.screens

import com.ritune.core.domain.usecases.coach.EndCoachConversation
import com.ritune.core.domain.usecases.coach.RunCoachTurn
import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
import com.ritune.core.domain.usecases.settings.ObserveSettings
import com.ritune.core.domain.usecases.settings.UpdateSettings
import com.ritune.core.models.AppSettings
import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.CoachException
import com.ritune.core.models.coach.CoachInput
import com.ritune.core.models.coach.CoachUsage
import com.ritune.core.models.coach.ConversationId
import com.ritune.core.models.coach.ModelEvent
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.coach.coachToolExecutor
import com.ritune.core.testing.id.SequentialIdGenerator
import com.ritune.core.testing.ports.FakeCoachGateway
import com.ritune.core.testing.repository.FakeSettingsRepository
import com.ritune.core.testing.rules.MainDispatcherRule
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CoachViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository(AppSettings(coachEnabled = true))
    private val gateway = FakeCoachGateway()
    private val weekRhythmCall = ToolCall(id = "call-1", name = WeekRhythmTool.DEFINITION.name)

    private fun TestScope.newViewModel(): CoachViewModel {
        val viewModel = CoachViewModel(
            ObserveSettings(settings),
            UpdateSettings(settings),
            RunCoachTurn(gateway, coachToolExecutor(FakeTimekeeper())),
            EndCoachConversation(gateway),
            SequentialIdGenerator("chat"),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect() }
        return viewModel
    }

    private fun CoachViewModel.chat() = assertIs<CoachUiState.Chat>(state.value)

    private fun CoachViewModel.texts() = chat().messages.map { it.fromUser to it.text }

    @Test
    fun coachOff_showsConsent_andTurningOnSavesTheSetting() = runTest {
        settings.settings.value = AppSettings(coachEnabled = false)
        val viewModel = newViewModel()

        assertEquals(CoachUiState.Consent, viewModel.state.value)

        viewModel.onEvent(CoachUiEvent.TurnOn)
        advanceUntilIdle()

        assertTrue(settings.settings.value.coachEnabled)
        assertEquals(CoachUiState.Chat(), viewModel.state.value)
    }

    @Test
    fun coachOff_sendsNothing() = runTest {
        settings.settings.value = AppSettings(coachEnabled = false)
        newViewModel()
        advanceUntilIdle()

        assertTrue(gateway.sent.isEmpty())
    }

    @Test
    fun send_streamsTheAnswerAfterTheQuestion() = runTest {
        gateway.reply(ModelEvent.Text(" Steady "), ModelEvent.Text("week."), ModelEvent.Finished(CoachUsage()))
        val viewModel = newViewModel()

        viewModel.onEvent(CoachUiEvent.InputChanged("How was my week?"))
        viewModel.onEvent(CoachUiEvent.Send)
        advanceUntilIdle()

        assertEquals(listOf(true to "How was my week?", false to "Steady week."), viewModel.texts())
        assertEquals("", viewModel.chat().input)
        assertFalse(viewModel.chat().answering)
    }

    @Test
    fun toolRunning_showsUntilTheAnswerStarts() = runTest {
        val gate = CompletableDeferred<Unit>()
        gateway.reply(ModelEvent.ToolCallRequest(weekRhythmCall), ModelEvent.Finished(CoachUsage()))
        gateway.replyAfter(gate, ModelEvent.Text("Steady week."), ModelEvent.Finished(CoachUsage()))
        val viewModel = newViewModel()

        viewModel.onEvent(CoachUiEvent.Ask("How was my week?"))
        advanceUntilIdle()

        assertEquals(WeekRhythmTool.DEFINITION.name, viewModel.chat().activeTool)
        assertTrue(viewModel.chat().answering)

        gate.complete(Unit)
        advanceUntilIdle()

        assertNull(viewModel.chat().activeTool)
        assertEquals(false to "Steady week.", viewModel.texts().last())
    }

    @Test
    fun failure_showsTheErrorAndStartsAFreshConversation() = runTest {
        gateway.failure = CoachException(CoachError.NETWORK)
        val viewModel = newViewModel()

        viewModel.onEvent(CoachUiEvent.Ask("hi"))
        advanceUntilIdle()

        assertEquals(CoachError.NETWORK, viewModel.chat().error)
        assertEquals(listOf(true to "hi"), viewModel.texts())
        assertEquals(listOf(ConversationId("chat-1")), gateway.closed)
    }

    @Test
    fun retry_asksTheLastQuestionAgainWithoutRepeatingIt() = runTest {
        gateway.failure = CoachException(CoachError.NETWORK)
        val viewModel = newViewModel()
        viewModel.onEvent(CoachUiEvent.Ask("hi"))
        advanceUntilIdle()

        gateway.failure = null
        gateway.reply(ModelEvent.Text("Hello."), ModelEvent.Finished(CoachUsage()))
        viewModel.onEvent(CoachUiEvent.Retry)
        advanceUntilIdle()

        assertEquals(listOf(true to "hi", false to "Hello."), viewModel.texts())
        assertNull(viewModel.chat().error)
        assertEquals(CoachInput.UserMessage("hi"), gateway.sent.last())
    }

    @Test
    fun stop_endsTheTurnAndForgetsTheConversation() = runTest {
        gateway.replyAfter(CompletableDeferred(), ModelEvent.Text("never"))
        val viewModel = newViewModel()
        viewModel.onEvent(CoachUiEvent.Ask("hi"))
        advanceUntilIdle()

        viewModel.onEvent(CoachUiEvent.Stop)
        advanceUntilIdle()

        assertFalse(viewModel.chat().answering)
        assertEquals(listOf(true to "hi"), viewModel.texts())
        assertEquals(listOf(ConversationId("chat-1")), gateway.closed)
    }

    @Test
    fun askWhileAnswering_isIgnored() = runTest {
        gateway.replyAfter(CompletableDeferred(), ModelEvent.Text("never"))
        val viewModel = newViewModel()
        viewModel.onEvent(CoachUiEvent.Ask("first"))
        advanceUntilIdle()

        viewModel.onEvent(CoachUiEvent.Ask("second"))

        assertEquals(listOf<CoachInput>(CoachInput.UserMessage("first")), gateway.sent)
    }
}
