package com.ritune.core.domain.usecases.settings

import com.ritune.core.domain.repositories.SettingsRepository
import com.ritune.core.models.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettings @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.observe()
}