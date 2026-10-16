package com.ritune.core.domain.ports

sealed interface StartResult {
    data object Started : StartResult
    data object AlreadyRunning : StartResult
    data object RoutineNotFound : StartResult
}
