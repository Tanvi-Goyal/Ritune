package com.ritune.core.domain.models.routine

import com.ritune.core.domain.usecases.routine.RoutineIssue
import com.ritune.core.models.Routine

sealed interface SaveResult {
    data class Saved(val routine: Routine) : SaveResult
    data class Invalid(val issues: Set<RoutineIssue>) : SaveResult
}