package com.ritune.feature.settings.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.models.ThemeSetting
import com.ritune.core.screenshottesting.captureScreenSizes
import com.ritune.core.screenshottesting.captureScreenVariants
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val content = SettingsUiState.Content(
        haptics = true,
        speech = false,
        sound = true,
        countdown = true,
        theme = ThemeSetting.SYSTEM,
        routineActive = false,
        themeSheetOpen = false,
        confirmingDelete = false,
        deleting = false,
    )

    @Composable
    private fun Screen(state: SettingsUiState) {
        RituneTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                SettingsContent(state, remember { SnackbarHostState() }, onEvent = {}, onBack = {})
            }
        }
    }

    @Test
    fun settings_acrossScreenSizes() {
        composeTestRule.captureScreenSizes("settings") { Screen(content) }
    }

    @Test
    fun settings_darkAndLargeFont() {
        composeTestRule.captureScreenVariants("settings") { Screen(content) }
    }

    // A tall phone, so the disabled delete row at the bottom is in view.
    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    @Config(qualifiers = "w400dp-h1000dp-420dpi")
    fun settings_routineActive() {
        composeTestRule.setContent { Screen(content.copy(routineActive = true)) }
        composeTestRule.waitForIdle()

        captureScreenRoboImage("src/test/screenshots/settings_routine_active.png")
    }

    // The sheet and the dialog draw in their own windows, which only a whole-screen capture includes.
    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    @Config(qualifiers = "w400dp-h500dp-420dpi")
    fun settings_themeSheet() {
        composeTestRule.setContent { Screen(content.copy(themeSheetOpen = true)) }
        composeTestRule.waitForIdle()

        captureScreenRoboImage("src/test/screenshots/settings_theme_sheet.png")
    }

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    @Config(qualifiers = "w400dp-h500dp-420dpi")
    fun settings_confirmDelete() {
        composeTestRule.setContent { Screen(content.copy(confirmingDelete = true)) }
        composeTestRule.waitForIdle()

        captureScreenRoboImage("src/test/screenshots/settings_confirm_delete.png")
    }
}
