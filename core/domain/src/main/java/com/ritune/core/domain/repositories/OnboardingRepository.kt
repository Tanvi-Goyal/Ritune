package com.ritune.core.domain.repositories

import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val isCompleted: Flow<Boolean>
    suspend fun setCompleted(completed: Boolean)
}