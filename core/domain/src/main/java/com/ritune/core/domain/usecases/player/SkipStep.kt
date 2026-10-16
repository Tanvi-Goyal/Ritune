package com.ritune.core.domain.usecases.player

import com.ritune.core.domain.ports.PlayerController
import javax.inject.Inject

class SkipStep @Inject constructor(
    private val player: PlayerController
) {
    suspend operator fun invoke() = player.skipStep()
}