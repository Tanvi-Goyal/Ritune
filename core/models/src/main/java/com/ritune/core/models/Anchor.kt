package com.ritune.core.models

import kotlinx.datetime.LocalTime

data class Anchor(
    val id: AnchorId,
    val name: String,
    val targetTime: LocalTime?,
)