package com.ritune.core.testing.repository

import com.ritune.core.domain.repositories.SettingsRepository
import com.ritune.core.models.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(
    settings: AppSettings = AppSettings(),
) : SettingsRepository {

    val settings = MutableStateFlow(settings)

    override fun observe(): Flow<AppSettings> = settings

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        settings.update(transform)
    }
}
