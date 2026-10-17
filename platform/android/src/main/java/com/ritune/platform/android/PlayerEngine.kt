package com.ritune.platform.android

import com.ritune.core.domain.usecases.player.RoutinePlayerReducer
import com.ritune.core.models.IdGenerator
import com.ritune.core.models.player.PlayerEffect
import com.ritune.core.models.player.PlayerEvent
import com.ritune.core.models.player.PlayerState
import com.ritune.core.models.Routine
import com.ritune.core.models.RunId
import com.ritune.core.models.Timekeeper
import com.ritune.core.models.player.Transition
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Process-scoped owner of player state and step timing.
 * Platform code supplies the process scope and collects [effects] to perform domain
 * or platform side effects.
 */
class PlayerEngine(
    private val timekeeper: Timekeeper,
    private val ids: IdGenerator,
    private val scope: CoroutineScope,
) : AutoCloseable {

    private sealed interface Command {
        data class Start(
            val routine: Routine,
            val result: CompletableDeferred<Boolean>,
        ) : Command

        data class Dispatch(
            val event: PlayerEvent,
            val result: CompletableDeferred<Unit>,
        ) : Command
    }

    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val effectChannel = Channel<PlayerEffect>(Channel.UNLIMITED)
    private val mutableState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    private var stepEndJob: Job? = null
    private val actor = scope.launch {
        for (command in commands) {
            when (command) {
                is Command.Start -> {
                    val transition = reduce(
                        PlayerEvent.Start(
                            routine = command.routine,
                            runId = RunId(ids.newId()),
                            at = timekeeper.now(),
                        )
                    )
                    command.result.complete(
                        transition.effects.any { it is PlayerEffect.RunStarted }
                    )
                }

                is Command.Dispatch -> {
                    reduce(command.event)
                    command.result.complete(Unit)
                }
            }
        }
    }

    val state: StateFlow<PlayerState> = mutableState.asStateFlow()
    val effects: Flow<PlayerEffect> = effectChannel.receiveAsFlow()

    suspend fun start(routine: Routine): Boolean {
        val result = CompletableDeferred<Boolean>()
        commands.send(Command.Start(routine, result))
        return result.await()
    }

    suspend fun pause() = dispatch(PlayerEvent.Pause(timekeeper.now()))
    suspend fun resume() = dispatch(PlayerEvent.Resume(timekeeper.now()))
    suspend fun skipStep() = dispatch(PlayerEvent.SkipStep(timekeeper.now()))
    suspend fun stop() = dispatch(PlayerEvent.Stop(timekeeper.now()))
    suspend fun reset() = dispatch(PlayerEvent.Reset)

    private suspend fun dispatch(event: PlayerEvent) {
        val result = CompletableDeferred<Unit>()
        commands.send(Command.Dispatch(event, result))
        result.await()
    }

    private suspend fun reduce(event: PlayerEvent): Transition {
        val transition = RoutinePlayerReducer.reduce(mutableState.value, event)
        mutableState.value = transition.state

        for (effect in transition.effects) {
            when (effect) {
                is PlayerEffect.ScheduleStepEnd -> scheduleStepEnd(effect.deadlineMono)
                PlayerEffect.CancelStepEnd -> cancelStepEnd()
                is PlayerEffect.RunStarted,
                is PlayerEffect.StepStarted,
                is PlayerEffect.RunFinished -> effectChannel.send(effect)
            }
        }
        return transition
    }

    private fun scheduleStepEnd(deadlineMono: Long) {
        stepEndJob?.cancel()
        stepEndJob = scope.launch {
            val waitMillis = (deadlineMono - timekeeper.now().monoMillis)
                .coerceAtLeast(0)
            delay(waitMillis.milliseconds)
            commands.send(
                Command.Dispatch(
                    PlayerEvent.StepElapsed(timekeeper.now()),
                    CompletableDeferred(),
                )
            )
        }
    }

    private fun cancelStepEnd() {
        stepEndJob?.cancel()
        stepEndJob = null
    }

    override fun close() {
        commands.close()
        effectChannel.close()
        actor.cancel()
        cancelStepEnd()
    }
}