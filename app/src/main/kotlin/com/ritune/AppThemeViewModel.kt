package com.ritune

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritune.core.designsystem.theme.ThemeMode
import com.ritune.core.domain.usecases.settings.ObserveSettings
import com.ritune.core.models.ThemeSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppThemeViewModel @Inject constructor(
    observeSettings: ObserveSettings
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode?> = observeSettings()
        .map { settings ->
            when (settings.theme) {
                ThemeSetting.SYSTEM -> ThemeMode.SYSTEM
                ThemeSetting.LIGHT -> ThemeMode.LIGHT
                ThemeSetting.DARK -> ThemeMode.DARK
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null
        )
}