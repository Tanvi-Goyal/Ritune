package com.ritune.core.domain.usecases.settings

import com.ritune.core.domain.repositories.SettingsRepository
import com.ritune.core.models.AppSettings
import javax.inject.Inject

class UpdateSettings @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(transform: (AppSettings) -> AppSettings) {
        settingsRepository.update(transform)
    }
}