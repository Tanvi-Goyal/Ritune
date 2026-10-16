package com.ritune.core.domain.models.rhythm

import com.ritune.core.models.Anchor

data class AnchorRhythm(
    val anchor: Anchor,
    val medianMinutes: Int?,
    val driftMinutes: Int?,
    val logsThisWeek: Int,
)
