package com.ritune.core.domain.models.player

import kotlin.time.Duration

data class RunProgress(
    val state: PlayerState,
    val remaining: Duration
)

