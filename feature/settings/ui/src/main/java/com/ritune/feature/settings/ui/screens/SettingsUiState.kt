package com.ritune.feature.settings.ui.screens

import com.ritune.core.models.ThemeSetting


sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Content(
        val haptics: Boolean,
        val speech: Boolean,
        val sound: Boolean,
        val countdown: Boolean,
        val theme: ThemeSetting,
        val routineActive: Boolean,
        val themeSheetOpen: Boolean,
        val confirmingDelete: Boolean,
        val deleting: Boolean,
    ) : SettingsUiState
}

sealed interface SettingsEvent {
    data class HapticsChanged(val enabled: Boolean) : SettingsEvent
    data class SpeechChanged(val enabled: Boolean) : SettingsEvent
    data class SoundChanged(val enabled: Boolean) : SettingsEvent
    data class CountdownChanged(val enabled: Boolean) : SettingsEvent
    data object ThemeClicked : SettingsEvent
    data class ThemeSelected(val theme: ThemeSetting) : SettingsEvent
    data object DismissTheme : SettingsEvent
    data object RequestDelete : SettingsEvent
    data object DismissDelete : SettingsEvent
    data object ConfirmDelete : SettingsEvent
}

sealed interface SettingsEffect {
    data object DataDeleted : SettingsEffect
    data object DeleteFailed : SettingsEffect
}