package com.ritune.feature.settings.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.models.ThemeSetting
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SettingsContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<SettingsEvent>()
    private var backClicks = 0

    private val content = SettingsUiState.Content(
        haptics = true,
        speech = false,
        sound = true,
        countdown = false,
        theme = ThemeSetting.DARK,
        routineActive = false,
        themeSheetOpen = false,
        confirmingDelete = false,
        deleting = false,
    )

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setContent(state: SettingsUiState) {
        composeTestRule.setContent {
            RituneTheme {
                SettingsContent(
                    state = state,
                    snackbarHostState = remember { SnackbarHostState() },
                    onEvent = { events += it },
                    onBack = { backClicks++ },
                )
            }
        }
    }

    @Test
    fun content_showsEachSettingValue() {
        setContent(content)

        composeTestRule.onNodeWithText(string(R.string.settings_haptics)).assertIsOn()
        composeTestRule.onNodeWithText(string(R.string.settings_speech)).assertIsOff()
        composeTestRule.onNodeWithText(string(R.string.settings_sound)).assertIsOn()
        composeTestRule.onNodeWithText(string(R.string.settings_countdown)).assertIsOff()
        composeTestRule.onNodeWithText(string(R.string.settings_theme_dark)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun toggles_sendTheFlippedValue() {
        setContent(content)

        composeTestRule.onNodeWithText(string(R.string.settings_haptics)).performClick()
        composeTestRule.onNodeWithText(string(R.string.settings_speech)).performClick()
        composeTestRule.onNodeWithText(string(R.string.settings_sound)).performClick()
        composeTestRule.onNodeWithText(string(R.string.settings_countdown)).performClick()

        assertEquals(
            listOf(
                SettingsEvent.HapticsChanged(false),
                SettingsEvent.SpeechChanged(true),
                SettingsEvent.SoundChanged(false),
                SettingsEvent.CountdownChanged(true),
            ),
            events,
        )
    }

    @Test
    fun backAndThemeRow_sendTheirActions() {
        setContent(content)

        composeTestRule.onNodeWithText(string(R.string.settings_back)).performClick()
        composeTestRule.onNodeWithText(string(R.string.settings_theme)).performScrollTo().performClick()

        assertEquals(1, backClicks)
        assertEquals(listOf<SettingsEvent>(SettingsEvent.ThemeClicked), events)
    }

    @Test
    fun themeSheet_marksCurrentThemeAndSelectsAnother() {
        setContent(content.copy(themeSheetOpen = true))

        composeTestRule.onNode(hasText(string(R.string.settings_theme_dark)) and isSelectable()).assertIsSelected()
        composeTestRule.onNodeWithText(string(R.string.settings_theme_light)).performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.ThemeSelected(ThemeSetting.LIGHT)), events)
    }

    @Test
    fun deleteRow_requestsDelete() {
        setContent(content)

        composeTestRule.onNodeWithText(string(R.string.settings_delete_hint)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.settings_delete)).performScrollTo().performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.RequestDelete), events)
    }

    @Test
    fun routineActive_disablesDeleteAndExplains() {
        setContent(content.copy(routineActive = true))

        composeTestRule.onNodeWithText(string(R.string.settings_delete_blocked)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.settings_delete)).assertIsNotEnabled().performClick()

        assertEquals(emptyList(), events)
    }

    @Test
    fun deleteDialog_cancelsOrConfirms() {
        setContent(content.copy(confirmingDelete = true))

        composeTestRule.onNodeWithText(string(R.string.settings_delete_body)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.settings_cancel)).performClick()
        composeTestRule.onNodeWithText(string(R.string.settings_delete_confirm)).performClick()

        assertEquals(listOf(SettingsEvent.DismissDelete, SettingsEvent.ConfirmDelete), events)
    }

    @Test
    fun loading_showsNothing() {
        setContent(SettingsUiState.Loading)

        composeTestRule.onNodeWithText(string(R.string.settings_title)).assertDoesNotExist()
    }
}
