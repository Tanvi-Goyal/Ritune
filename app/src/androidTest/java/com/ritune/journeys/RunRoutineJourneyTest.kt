package com.ritune.journeys

import android.Manifest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.rule.GrantPermissionRule
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

/** A user starts a routine from its detail screen, skips through it, and lands back where they started. */
@HiltAndroidTest
class RunRoutineJourneyTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hiltRule)
        .around(GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS))
        .around(composeTestRule)

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun waitFor(matcher: SemanticsMatcher) {
        composeTestRule.waitUntil(TIMEOUT_MS) {
            composeTestRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tap(label: Int) = composeTestRule.onNode(screenText(string(label))).performClick()

    @Test
    fun skipThroughRoutine_showsSummaryAndDoneReturnsToDetail() {
        waitFor(screenText(string(R.string.today_title)))
        composeTestRule.onNode(bottomBarItem(string(R.string.nav_routines))).performClick()
        waitFor(screenText(string(R.string.routines_title)))
        composeTestRule.onNode(screenText("Wind down") and hasClickAction()).performScrollTo().performClick()
        waitFor(screenText(string(R.string.detail_edit)))

        tap(R.string.detail_start)
        waitFor(screenText(string(R.string.player_step_counter, 1, 3)))
        tap(R.string.player_skip)
        waitFor(screenText(string(R.string.player_step_counter, 2, 3)))
        tap(R.string.player_skip)
        waitFor(screenText(string(R.string.player_step_counter, 3, 3)))
        tap(R.string.player_finish)

        waitFor(screenText(string(R.string.summary_title_skipped, "Wind down")))
        tap(R.string.summary_done)

        waitFor(screenText(string(R.string.detail_edit)))
        composeTestRule.onNode(screenText(string(R.string.player_stop))).assertDoesNotExist()
        composeTestRule.onNode(screenText(string(R.string.summary_done))).assertDoesNotExist()
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
