package com.ritune.navigation

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
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
class RituneNavigationTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hiltRule).around(composeTestRule)

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun pressBack() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
    }

    private fun waitForScreen(title: Int) {
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodes(screenText(string(title))).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun launch_opensTodayWithBottomBar() {
        waitForScreen(R.string.today_title)

        composeTestRule.onNodeWithTag("bottomBar").assertIsDisplayed()
    }

    @Test
    fun bottomBar_switchesBetweenTopLevelScreens() {
        waitForScreen(R.string.today_title)

        composeTestRule.onNode(bottomBarItem(string(R.string.nav_rhythm))).performClick()
        waitForScreen(R.string.rhythm_title)

        composeTestRule.onNode(bottomBarItem(string(R.string.nav_routines))).performClick()
        waitForScreen(R.string.routines_title)

        composeTestRule.onNode(bottomBarItem(string(R.string.nav_today))).performClick()
        waitForScreen(R.string.today_title)
    }

    @Test
    fun back_fromTopLevelTab_returnsToToday() {
        waitForScreen(R.string.today_title)
        composeTestRule.onNode(bottomBarItem(string(R.string.nav_rhythm))).performClick()
        waitForScreen(R.string.rhythm_title)

        pressBack()

        waitForScreen(R.string.today_title)
    }

    @Test
    fun settings_hidesBottomBarAndBackReturns() {
        waitForScreen(R.string.today_title)

        composeTestRule.onNode(screenText(string(R.string.today_settings))).performClick()
        waitForScreen(R.string.settings_title)
        composeTestRule.onNodeWithTag("bottomBar").assertDoesNotExist()

        pressBack()

        waitForScreen(R.string.today_title)
        composeTestRule.onNodeWithTag("bottomBar").assertIsDisplayed()
    }
}

@HiltAndroidTest
class RituneDeepLinkTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hiltRule).around(composeTestRule)

    @Test
    fun summaryDeepLink_unknownRun_showsNotFound() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("${DeepLinks.SUMMARY}/missing-run"))

        ActivityScenario.launch<ComponentActivity>(intent).use {
            val notFound = context.getString(R.string.summary_not_found_title)
            composeTestRule.waitUntil(10_000) {
                composeTestRule.onAllNodes(screenText(notFound)).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}
