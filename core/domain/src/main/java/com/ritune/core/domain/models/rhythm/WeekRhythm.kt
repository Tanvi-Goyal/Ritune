package com.ritune.core.domain.models.rhythm

import com.ritune.core.models.Anchor

data class WeekRhythm(
    val days: List<DayConsistency>,
    val consistency: Float?,
    val steadiest: Anchor?,
    val anchors: List<AnchorRhythm>,
    val loggedDays: Int,
    val hasEnoughData: Boolean,
)
