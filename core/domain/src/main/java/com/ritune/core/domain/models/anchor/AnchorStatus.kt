package com.ritune.core.domain.models.anchor

import com.ritune.core.models.Anchor
import kotlin.time.Instant

data class AnchorStatus(
    val anchor: Anchor,
    val loggedAt: Instant?
)