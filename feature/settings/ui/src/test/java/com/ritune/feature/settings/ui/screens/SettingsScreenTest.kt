package com.ritune.feature.settings.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.domain.usecases.DeleteAllData
import com.ritune.core.domain.usecases.player.ObservePlayerState
import com.ritune.core.domain.usecases.settings.ObserveSettings
import com.ritune.core.domain.usecases.settings.UpdateSettings
import com.ritune.core.testing.ports.FakeDataResetter
import com.ritune.core.testing.ports.FakePlayerController
import com.ritune.core.testing.repository.FakeSettingsRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val resetter = FakeDataResetter()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun deleteAllData() {
        val settings = FakeSettingsRepository()
        val viewModel = SettingsViewModel(
            ObserveSettings(settings),
            UpdateSettings(settings),
            ObservePlayerState(FakePlayerController()),
            DeleteAllData(resetter),
        )
        composeTestRule.setContent {
            RituneTheme { SettingsScreen(viewModel, onBack = {}) }
        }
        composeTestRule.onNodeWithText(string(R.string.settings_delete)).performScrollTo().performClick()
        composeTestRule.onNodeWithText(string(R.string.settings_delete_confirm)).performClick()
    }

    @Test
    fun deleteSucceeds_showsDeletedMessage() {
        deleteAllData()

        composeTestRule.onNodeWithText(string(R.string.settings_deleted)).assertIsDisplayed()
    }

    @Test
    fun deleteFails_showsRetryMessage() {
        resetter.error = IOException("disk full")

        deleteAllData()

        composeTestRule.onNodeWithText(string(R.string.settings_delete_failed)).assertIsDisplayed()
    }
}
