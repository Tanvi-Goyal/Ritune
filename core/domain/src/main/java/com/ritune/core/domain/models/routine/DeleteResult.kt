package com.ritune.core.domain.models.routine

sealed interface DeleteResult {
    data object Deleted : DeleteResult
    data object NotFound : DeleteResult
    data object BuiltIn : DeleteResult
}