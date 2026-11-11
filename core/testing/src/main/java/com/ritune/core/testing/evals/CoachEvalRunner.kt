package com.ritune.core.testing.evals

import com.ritune.core.domain.ports.CoachGateway
import com.ritune.core.domain.usecases.coach.CoachInstructions
import com.ritune.core.domain.usecases.coach.RunCoachTurn
import com.ritune.core.models.coach.AppliedChange
import com.ritune.core.models.coach.CoachError
import com.ritune.core.models.coach.CoachEvent
import com.ritune.core.models.coach.CoachInput
import com.ritune.core.models.coach.CoachSetup
import com.ritune.core.models.coach.CoachUsage
import com.ritune.core.models.coach.ConversationId
import com.ritune.core.models.coach.ModelEvent
import com.ritune.core.models.coach.Proposal
import com.ritune.core.models.coach.ProposalOutcome
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.models.coach.ToolResult
import com.ritune.core.testing.coach.applyCoachProposal
import com.ritune.core.testing.coach.coachToolExecutor
import com.ritune.core.testing.coach.undoCoachChange
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.repository.FakeRoutineRepository
import com.ritune.core.testing.repository.FakeRunHistoryRepository
import com.ritune.core.testing.time.FakeTimekeeper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class CoachEvalRunner(
    private val gateway: CoachGateway,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val pause: Duration = 4.seconds,
    private val retryWait: Duration = 60.seconds,
    private val attempts: Int = 3,
) {
    suspend fun run(
        scenarios: List<EvalScenario>,
        onResult: (ScenarioResult) -> Unit = {},
    ): EvalReport {
        val results = mutableListOf<ScenarioResult>()
        for (scenario in scenarios) {
            if (results.isNotEmpty()) delay(pause)
            results += runWithRetries(scenario).also(onResult)
            // Every scenario failing with an error means a setup problem, such as App Check.
            if (results.size == SETUP_CHECK && results.all { it.failedWithError }) break
        }
        return EvalReport(CoachInstructions.VERSION, results, planned = scenarios.size)
    }

    private companion object {
        const val SETUP_CHECK = 3
    }

    private suspend fun runWithRetries(scenario: EvalScenario): ScenarioResult {
        var result = runOnce(scenario, attempt = 1)
        while (result.retryable && result.attempts < attempts) {
            delay(retryWait)
            result = runOnce(scenario, result.attempts + 1)
        }
        return result
    }

    private suspend fun runOnce(scenario: EvalScenario, attempt: Int): ScenarioResult {
        val world = scenario.world
        val timekeeper = FakeTimekeeper(wall = world.now.toInstant(TimeZone.UTC))
        val anchors = FakeAnchorRepository(world.anchors)
        val events = FakeAnchorEventRepository(world.events)
        val routines = FakeRoutineRepository(world.routines)
        val runs = FakeRunHistoryRepository(world.runs)
        val recorder = RecordingGateway(gateway)
        val runTurn = RunCoachTurn(
            recorder,
            coachToolExecutor(timekeeper, anchors, events, routines, runs),
            applyCoachProposal(timekeeper, anchors, events, routines),
        )
        val undo = undoCoachChange(anchors, events, routines)
        val conversation = ConversationId("eval-${scenario.id}-$attempt")

        val turns = mutableListOf<TurnResult>()
        val sources = StringBuilder(CoachInstructions.text)
        var undone = emptyList<Proposal>()
        try {
            for (turn in scenario.turns) {
                recorder.reset()
                sources.appendLine().append(turn.question)
                val proposals = mutableListOf<Proposal>()
                var applied: AppliedChange? = null
                var error: CoachError? = null
                var usage = CoachUsage()
                val reply = StringBuilder()
                val started = timeSource.markNow()
                runTurn(
                    conversation,
                    turn.question,
                    approve = { proposals += it; turn.decision },
                    undone = undone,
                ).collect { event ->
                    when (event) {
                        is CoachEvent.TextDelta -> reply.append(event.text)
                        is CoachEvent.ProposalResolved ->
                            applied = (event.outcome as? ProposalOutcome.Applied)?.change

                        is CoachEvent.Done -> usage = event.usage
                        is CoachEvent.Failed -> error = event.error
                        is CoachEvent.ToolRunning -> Unit
                    }
                }
                val latency = started.elapsedNow()
                recorder.results.forEach { sources.appendLine().append(it.json) }
                recorder.calls.forEach { sources.appendLine().append(it.arguments.values) }
                val record = TurnRecord(
                    reply = reply.toString(),
                    calls = recorder.calls.toList(),
                    results = recorder.results.toList(),
                    proposals = proposals,
                    error = error,
                    rounds = recorder.requests - 1,
                )
                turns += TurnResult(
                    turn, record, record.check(
                        turn.expect,
                        sources.toString()
                    ),
                    latency, usage
                )
                if (error != null) break

                undone = emptyList()
                val change = applied
                if (turn.undoAfter && change != null) {
                    undo(change)
                    undone = listOf(change.proposal)
                    // The next question carries an undo note with the restored value.
                    sources.appendLine().append(change.proposal)
                }
            }
        } finally {
            gateway.close(conversation)
        }
        return ScenarioResult(scenario, turns, attempt)
    }
}

private class RecordingGateway(private val real: CoachGateway) : CoachGateway {
    val calls = mutableListOf<ToolCall>()
    val results = mutableListOf<ToolResult>()
    var requests = 0
        private set

    fun reset() {
        calls.clear()
        results.clear()
        requests = 0
    }

    override fun send(
        conversation: ConversationId,
        setup: CoachSetup,
        input: CoachInput,
    ): Flow<ModelEvent> {
        requests++
        if (input is CoachInput.ToolResults) results += input.results
        return real.send(conversation, setup, input).onEach {
            if (it is ModelEvent.ToolCallRequest) calls += it.call
        }
    }

    override fun close(conversation: ConversationId) = real.close(conversation)

    override suspend fun forgetUser() = real.forgetUser()
}

data class TurnResult(
    val turn: EvalTurn,
    val record: TurnRecord,
    val checks: List<CheckResult>,
    val latency: Duration,
    val usage: CoachUsage,
) {
    val passed get() = checks.all { it.passed }
}

data class ScenarioResult(
    val scenario: EvalScenario,
    val turns: List<TurnResult>,
    val attempts: Int,
) {
    val passed get() = turns.size == scenario.turns.size && turns.all { it.passed }
    val failures get() = turns.flatMap { turn -> turn.checks.filterNot { it.passed } }
    val latency get() = turns.fold(Duration.ZERO) { total, turn -> total + turn.latency }
    val usage get() = turns.fold(CoachUsage()) { total, turn -> total + turn.usage }

    internal val failedWithError get() = turns.any { it.record.error != null }

    internal val retryable
        get() = turns.lastOrNull()?.record?.error in setOf(
            CoachError.QUOTA_EXCEEDED,
            CoachError.NETWORK
        )
}
