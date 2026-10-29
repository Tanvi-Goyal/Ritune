package com.ritune.journeys

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
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

/** A user creates a routine with one step, finds it in the list, then renames it. */
@HiltAndroidTest
class EditRoutineJourneyTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hiltRule).around(composeTestRule)

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun waitFor(matcher: SemanticsMatcher) {
        composeTestRule.waitUntil(TIMEOUT_MS) {
            composeTestRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun field(label: Int) = composeTestRule.onNode(hasSetTextAction() and hasText(string(label)))

    private fun tap(label: Int) {
        val target = screenText(string(label))
        val scrollable = composeTestRule.onAllNodes(target and hasAnyAncestor(hasScrollAction()))
            .fetchSemanticsNodes().isNotEmpty()
        composeTestRule.onNode(target).apply { if (scrollable) performScrollTo() }.performClick()
    }

    @Test
    fun createThenRename_showsTheRoutineUnderItsNewName() {
        waitFor(screenText(string(R.string.today_title)))
        composeTestRule.onNode(bottomBarItem(string(R.string.nav_routines))).performClick()
        waitFor(screenText(string(R.string.routines_title)))

        tap(R.string.routines_new)
        waitFor(screenText(string(R.string.editor_title_new)))
        field(R.string.editor_name_label).performTextInput("Desk reset")
        tap(R.string.editor_add_step)
        waitFor(screenText(string(R.string.editor_step_sheet_new)))
        field(R.string.editor_step_title).performTextInput("Clear the desk")
        composeTestRule.onNode(screenText(string(R.string.editor_step_done))).performClick()
        waitFor(screenText("Clear the desk", substring = true))
        composeTestRule.onNode(screenText(string(R.string.editor_save))).performClick()

        val created = screenText("Desk reset") and hasClickAction()
        waitFor(created)
        composeTestRule.onNode(created).performScrollTo().performClick()
        waitFor(screenText(string(R.string.detail_edit)))
        tap(R.string.detail_edit)
        waitFor(screenText(string(R.string.editor_title_edit)))
        field(R.string.editor_name_label).performTextReplacement("Evening desk reset")
        composeTestRule.onNode(screenText(string(R.string.editor_save))).performClick()

        waitFor(screenText(string(R.string.detail_edit)))
        waitFor(screenText("Evening desk reset"))
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
