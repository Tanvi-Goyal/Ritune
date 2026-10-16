package com.ritune.core.domain.models.anchor

import com.ritune.core.models.AnchorEvent

sealed interface LogResult {
    data class Logged(val event: AnchorEvent) : LogResult
    data class Duplicate(val existing: AnchorEvent) : LogResult
}