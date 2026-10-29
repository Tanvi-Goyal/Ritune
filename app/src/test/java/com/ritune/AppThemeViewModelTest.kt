package com.ritune

import com.ritune.core.designsystem.theme.ThemeMode
import com.ritune.core.domain.usecases.settings.ObserveSettings
import com.ritune.core.models.AppSettings
import com.ritune.core.models.ThemeSetting
import com.ritune.core.testing.repository.FakeSettingsRepository
import com.ritune.core.testing.rules.MainDispatcherRule
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AppThemeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository(AppSettings(theme = ThemeSetting.SYSTEM))

    @Test
    fun themeMode_followsTheSavedThemeSetting() {
        val viewModel = AppThemeViewModel(ObserveSettings(settings))
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        settings.settings.update { it.copy(theme = ThemeSetting.LIGHT) }
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)

        settings.settings.update { it.copy(theme = ThemeSetting.DARK) }
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }
}
