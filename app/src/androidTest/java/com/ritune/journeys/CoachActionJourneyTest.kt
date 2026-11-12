package com.ritune.journeys

import android.text.format.DateFormat
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.ritune.MainActivity
import com.ritune.bottomBarItem
import com.ritune.core.designsystem.R
import com.ritune.core.domain.usecases.coach.tools.ProposeAnchorTargetTool
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.coach.CoachUsage
import com.ritune.core.models.coach.ModelEvent
import com.ritune.core.models.coach.ToolCall
import com.ritune.core.testing.ports.FakeCoachGateway
import com.ritune.screenText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** The coach proposes a later Screens off target, the user applies it, and Today shows the new target. */
@HiltAndroidTest
class CoachActionJourneyTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hiltRule).around(composeTestRule)

    @Inject
    lateinit var gateway: FakeCoachGateway

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun string(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    private fun waitFor(matcher: SemanticsMatcher) {
        composeTestRule.waitUntil(TIMEOUT_MS) {
            composeTestRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // Same pattern as the app's time formatter, so the test passes in 12 and 24 hour locales.
    private fun target(hour: Int, minute: Int): String {
        val activity = composeTestRule.activity
        val locale = activity.resources.configuration.locales[0]
        val pattern = DateFormat.getBestDateTimePattern(
            locale,
            if (DateFormat.is24HourFormat(activity)) "Hm" else "hm"
        )
        return string(
            R.string.today_target_time,
            DateTimeFormatter.ofPattern(pattern, locale).format(LocalTime.of(hour, minute))
        )
    }

    private val shutdownRow get() = screenText("Screens off", substring = true) and hasClickAction()

    @Test
    fun applyProposedTarget_updatesToday() {
        gateway.reply(
            ModelEvent.ToolCallRequest(
                ToolCall(
                    id = "call-1",
                    name = ProposeAnchorTargetTool.DEFINITION.name,
                    arguments = mapOf(
                        "anchor_id" to DefaultCatalog.shutdown.id.value,
                        "target_time" to "22:00"
                    ),
                ),
            ),
            ModelEvent.Finished(CoachUsage()),
        )
        gateway.reply(
            ModelEvent.Text("Done, Screens off is now 22:00."),
            ModelEvent.Finished(CoachUsage())
        )

        waitFor(shutdownRow)
        composeTestRule.onNode(bottomBarItem(string(R.string.nav_rhythm))).performClick()
        waitFor(screenText(string(R.string.rhythm_ask_coach)))
        composeTestRule.onNode(screenText(string(R.string.rhythm_ask_coach)) and hasClickAction())
            .performScrollTo().performClick()

        waitFor(screenText(string(R.string.coach_consent_turn_on)))
        composeTestRule.onNode(screenText(string(R.string.coach_consent_turn_on))).performScrollTo()
            .performClick()

        waitFor(hasSetTextAction())
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Move screens off to 22:00")
        composeTestRule.onNode(screenText(string(R.string.coach_send))).performClick()

        waitFor(screenText(string(R.string.coach_proposal_apply)))
        composeTestRule.onNode(screenText(string(R.string.coach_proposal_apply))).performClick()
        waitFor(screenText(string(R.string.coach_proposal_applied)))
        waitFor(screenText("Done, Screens off is now 22:00."))

        composeTestRule.onNode(screenText(string(R.string.coach_back))).performClick()
        composeTestRule.onNode(bottomBarItem(string(R.string.nav_today))).performClick()

        waitFor(shutdownRow and hasText(target(22, 0), substring = true))
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
