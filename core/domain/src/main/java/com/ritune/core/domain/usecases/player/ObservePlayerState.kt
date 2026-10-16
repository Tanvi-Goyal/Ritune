package com.ritune.core.domain.usecases.player

import com.ritune.core.domain.models.player.PlayerState
import com.ritune.core.domain.ports.PlayerController
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObservePlayerState @Inject constructor(
    private val player: PlayerController
) {
    operator fun invoke(): StateFlow<PlayerState> = player.state
}