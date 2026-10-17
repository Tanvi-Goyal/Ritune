package com.ritune.core.domain.ports

import com.ritune.core.models.player.PlayerState
import com.ritune.core.models.Routine
import kotlinx.coroutines.flow.StateFlow

interface PlayerController {
    val state: StateFlow<PlayerState>
    suspend fun start(routine: Routine): StartResult
    suspend fun pause()
    suspend fun resume()
    suspend fun skipStep()
    suspend fun stop()
    suspend fun reset()
}