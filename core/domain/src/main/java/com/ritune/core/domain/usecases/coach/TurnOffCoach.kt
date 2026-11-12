package com.ritune.core.domain.usecases.coach

import com.ritune.core.domain.ports.CoachGateway
import com.ritune.core.domain.repositories.SettingsRepository
import javax.inject.Inject

class TurnOffCoach @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val coach: CoachGateway,
) {
    suspend operator fun invoke() {
        settingsRepository.update { it.copy(coachEnabled = false) }
        coach.forgetUser()
    }
}
