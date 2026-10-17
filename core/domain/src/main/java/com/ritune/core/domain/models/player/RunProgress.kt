package com.ritune.core.domain.models.player

import com.ritune.core.models.player.PlayerState
import kotlin.time.Duration

data class RunProgress(
    val state: PlayerState,
    val remaining: Duration
)

