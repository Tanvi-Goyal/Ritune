package com.ritune.core.testing.repository

import com.ritune.core.domain.repositories.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeOnboardingRepository(completed: Boolean = false) : OnboardingRepository {

    override val isCompleted = MutableStateFlow(completed)

    override suspend fun setCompleted(completed: Boolean) {
        isCompleted.value = completed
    }
}
