package com.ritune.ailab

import com.ritune.core.testing.rules.MainDispatcherRule
import com.ritune.core.testing.time.VirtualTimekeeper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AiLabViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val client = FakeLabChatClient()

    private fun TestScope.newViewModel() = AiLabViewModel(client, VirtualTimekeeper(testScheduler))

    @Test
    fun send_streamsReplyAndRecordsTiming() = runTest {
        client.delayMillis = 300
        client.chunks = listOf(
            LabChunk("Hel"),
            LabChunk("lo", LabUsage(inputTokens = 2, outputTokens = 9, thinkingTokens = 0)),
        )
        val viewModel = newViewModel()

        viewModel.onEvent(AiLabEvent.InputChanged("hi"))
        viewModel.onEvent(AiLabEvent.Send)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Hello", state.reply)
        assertFalse(state.streaming)
        assertEquals(
            LabStats(
                firstChunkMillis = 300,
                totalMillis = 600,
                inputTokens = 2,
                outputTokens = 9,
                thinkingTokens = 0,
            ),
            state.stats,
        )
        assertEquals(
            LabRequest("hi", LabModel.FLASH_LITE_3_5, LabThinking.DEFAULT, rememberChat = false),
            client.requests.single(),
        )
    }

    @Test
    fun send_withBlankInput_doesNothing() = runTest {
        val viewModel = newViewModel()

        viewModel.onEvent(AiLabEvent.InputChanged("   "))
        viewModel.onEvent(AiLabEvent.Send)
        advanceUntilIdle()

        assertTrue(client.requests.isEmpty())
    }

    @Test
    fun send_whenTheCallFails_showsTheErrorAndStops() = runTest {
        client.failure = IOException("offline")
        val viewModel = newViewModel()

        viewModel.onEvent(AiLabEvent.InputChanged("hi"))
        viewModel.onEvent(AiLabEvent.Send)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("IOException: offline", state.error)
        assertFalse(state.streaming)
        assertNull(state.stats)
    }

    @Test
    fun stop_cancelsTheReply() = runTest {
        client.neverFinishes = true
        val viewModel = newViewModel()
        viewModel.onEvent(AiLabEvent.InputChanged("hi"))
        viewModel.onEvent(AiLabEvent.Send)
        runCurrent()
        assertTrue(viewModel.state.value.streaming)

        viewModel.onEvent(AiLabEvent.Stop)
        runCurrent()

        assertFalse(viewModel.state.value.streaming)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun changingASetting_startsANewChat() = runTest {
        val viewModel = newViewModel()

        viewModel.onEvent(AiLabEvent.ModelSelected(LabModel.FLASH_3_5))
        viewModel.onEvent(AiLabEvent.ThinkingSelected(LabThinking.LOW))
        viewModel.onEvent(AiLabEvent.RememberChatChanged(true))

        assertEquals(3, client.historyCleared)
        val state = viewModel.state.value
        assertEquals(LabModel.FLASH_3_5, state.model)
        assertEquals(LabThinking.LOW, state.thinking)
        assertTrue(state.rememberChat)
    }

    @Test
    fun runBatch_sendsThePromptEachTimeWithoutHistory() = runTest {
        client.chunks = listOf(LabChunk("Hello"))
        val viewModel = newViewModel()
        viewModel.onEvent(AiLabEvent.RememberChatChanged(true))

        viewModel.onEvent(
            AiLabEvent.RunBatch("hi", LabModel.FLASH_3_5, LabThinking.MINIMAL, runs = 3),
        )
        advanceUntilIdle()

        val expected = LabRequest("hi", LabModel.FLASH_3_5, LabThinking.MINIMAL, rememberChat = false)
        assertEquals(List(3) { expected }, client.requests)
        assertEquals("Hello", viewModel.state.value.reply)
    }
}
