package com.ritune.core.domain.usecases.player

import com.ritune.core.domain.ports.PlayerController
import javax.inject.Inject

class StopRun @Inject constructor(
    private val player: PlayerController
) {
    suspend operator fun invoke() = player.stop()
}