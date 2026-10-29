package com.ritune.navigation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.ritune.MainActivity
import com.ritune.bottomBarItem
import com.ritune.core.designsystem.R
import com.ritune.core.navigation.DeepLinks
import com.ritune.screenText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

@HiltAndroidTest
class RoutineNavigationTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hiltRule).around(composeTestRule)

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun waitFor(matcher: SemanticsMatcher) {
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun routineDetail_opensFromRoutinesAndBackReturns() {
        waitFor(screenText(string(R.string.today_title)))
        composeTestRule.onNode(bottomBarItem(string(R.string.nav_routines))).performClick()
        waitFor(screenText(string(R.string.routines_title)))

        composeTestRule.onNode(screenText("Morning start") and hasClickAction()).performScrollTo().performClick()
        waitFor(screenText(string(R.string.detail_edit)))
        composeTestRule.onNodeWithTag("bottomBar").assertDoesNotExist()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()

        waitFor(screenText(string(R.string.routines_title)))
        composeTestRule.onNodeWithTag("bottomBar").assertIsDisplayed()
    }
}

@HiltAndroidTest
class PlayerDeepLinkTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hiltRule)
        .around(GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS))
        .around(composeTestRule)

    @Test
    fun playerDeepLink_nothingRunning_closesToTodayWithoutStartingAnything() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse(DeepLinks.PLAYER))

        ActivityScenario.launch<ComponentActivity>(intent).use {
            val today = screenText(context.getString(R.string.today_title))
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(today).fetchSemanticsNodes().isNotEmpty()
            }

            composeTestRule.onNodeWithTag("bottomBar").assertIsDisplayed()
            composeTestRule.onNode(screenText(context.getString(R.string.today_running_open))).assertDoesNotExist()
        }
    }
}
