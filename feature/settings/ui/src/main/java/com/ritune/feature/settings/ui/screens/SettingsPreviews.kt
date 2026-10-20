package com.ritune.feature.settings.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.models.ThemeSetting

private fun content(
    theme: ThemeSetting = ThemeSetting.SYSTEM,
    routineActive: Boolean = false,
    confirmingDelete: Boolean = false,
    sound: Boolean = false,
) = SettingsUiState.Content(
    haptics = true,
    speech = true,
    sound = sound,
    countdown = false,
    theme = theme,
    routineActive = routineActive,
    themeSheetOpen = false,
    confirmingDelete = confirmingDelete,
    deleting = false,
)

@Composable
private fun Preview(state: SettingsUiState) {
    RituneTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsContent(
                state,
                remember { SnackbarHostState() },
                onEvent = {},
                onBack = {},
                onOpenReminders = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun SettingsDefaultPreview() = Preview(content())

@PreviewLightDark
@Composable
private fun SettingsAllOnDarkThemePreview() =
    Preview(content(theme = ThemeSetting.DARK, sound = true))

@PreviewLightDark
@Composable
private fun SettingsRoutineRunningPreview() = Preview(content(routineActive = true))

@PreviewLightDark
@Composable
private fun SettingsConfirmDeletePreview() = Preview(content(confirmingDelete = true))