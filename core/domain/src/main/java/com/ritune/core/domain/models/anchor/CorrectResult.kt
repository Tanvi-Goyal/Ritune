package com.ritune.core.domain.models.anchor

import com.ritune.core.models.AnchorEvent

sealed interface CorrectResult {
    data class Corrected(val event: AnchorEvent) : CorrectResult
    data object InFuture : CorrectResult
}