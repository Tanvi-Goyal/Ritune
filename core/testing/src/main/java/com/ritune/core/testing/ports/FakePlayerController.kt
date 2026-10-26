package com.ritune.core.testing.ports

import com.ritune.core.domain.ports.PlayerController
import com.ritune.core.domain.ports.StartResult
import com.ritune.core.models.Routine
import com.ritune.core.models.player.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow

class FakePlayerController : PlayerController {

    enum class Command { PAUSE, RESUME, SKIP_STEP, STOP, RESET }

    override val state = MutableStateFlow<PlayerState>(PlayerState.Idle)

    var startResult: StartResult = StartResult.Started

    val startedRoutines = mutableListOf<Routine>()
    val commands = mutableListOf<Command>()

    override suspend fun start(routine: Routine): StartResult {
        startedRoutines += routine
        return startResult
    }

    override suspend fun pause() {
        commands += Command.PAUSE
    }

    override suspend fun resume() {
        commands += Command.RESUME
    }

    override suspend fun skipStep() {
        commands += Command.SKIP_STEP
    }

    override suspend fun stop() {
        commands += Command.STOP
    }

    override suspend fun reset() {
        commands += Command.RESET
    }
}
