package com.ritune.journeys

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import com.ritune.MainActivity
import com.ritune.bottomBarItem
import com.ritune.core.designsystem.R
import com.ritune.screenText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

/** A user logs their wake time, undoes it, logs it again, and finds it still logged after switching tabs. */
@HiltAndroidTest
class LogAnchorJourneyTest {

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

    private val wakeRow = screenText("Wake", substring = true) and hasClickAction()
    private val nothingLogged get() = screenText(string(R.string.today_headline_empty))
    private val oneLogged get() = screenText(string(R.string.today_subtitle_partial, 1, 6))

    @Test
    fun logUndoAndRelog_survivesTabSwitch() {
        waitFor(nothingLogged)

        composeTestRule.onNode(wakeRow).performClick()
        waitFor(oneLogged)

        composeTestRule.onNode(screenText(string(R.string.today_snackbar_undo))).performClick()
        waitFor(nothingLogged)

        composeTestRule.onNode(wakeRow).performClick()
        waitFor(oneLogged)

        composeTestRule.onNode(bottomBarItem(string(R.string.nav_rhythm))).performClick()
        waitFor(screenText(string(R.string.rhythm_title)))
        composeTestRule.onNode(bottomBarItem(string(R.string.nav_today))).performClick()

        waitFor(oneLogged)
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
