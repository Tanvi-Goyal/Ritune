package com.ritune.journeys

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ritune.MainActivity
import com.ritune.core.designsystem.R
import com.ritune.screenText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

/** A user logs an anchor, deletes all data from Settings, and comes back to an empty Today. */
@HiltAndroidTest
class ResetDataJourneyTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hiltRule).around(composeTestRule)

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun waitFor(matcher: SemanticsMatcher) {
        composeTestRule.waitUntil(TIMEOUT_MS) {
            composeTestRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private val nothingLogged get() = screenText(string(R.string.today_headline_empty))

    @Test
    fun deleteAllData_clearsTodaysLogs() {
        waitFor(nothingLogged)
        composeTestRule.onNode(screenText("Wake", substring = true) and hasClickAction()).performClick()
        waitFor(screenText(string(R.string.today_subtitle_partial, 1, 6)))

        composeTestRule.onNode(screenText(string(R.string.today_settings))).performClick()
        waitFor(screenText(string(R.string.settings_title)))
        composeTestRule.onNode(screenText(string(R.string.settings_delete))).performScrollTo().performClick()
        waitFor(screenText(string(R.string.settings_delete_title)))
        composeTestRule.onNode(screenText(string(R.string.settings_delete_confirm))).performClick()
        waitFor(screenText(string(R.string.settings_deleted)))

        composeTestRule.onNode(screenText(string(R.string.settings_back))).performClick()

        waitFor(nothingLogged)
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
