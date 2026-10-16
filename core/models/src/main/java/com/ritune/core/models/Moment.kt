package com.ritune.core.models

import kotlin.time.Instant

data class Moment(
    val monoMillis: Long,
    val wall: Instant
)


