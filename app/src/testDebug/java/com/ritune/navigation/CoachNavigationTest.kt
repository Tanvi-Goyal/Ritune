package com.ritune.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.feature.coach.ui.navigation.CoachRoute
import com.ritune.feature.rhythm.ui.navigation.RhythmRoute
import com.ritune.feature.today.ui.navigation.TodayRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class CoachNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController

    private fun setUpGraph() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            NavHost(navController, startDestination = TodayRoute) {
                composable<TodayRoute> {}
                composable<RhythmRoute> {}
                composable<CoachRoute> {}
            }
        }
    }

    private fun navigate(command: TestNavHostController.() -> Unit) {
        composeTestRule.runOnIdle { navController.command() }
        composeTestRule.waitForIdle()
    }

    private fun backStack() = navController.currentBackStack.value.mapNotNull { entry ->
        listOf(TodayRoute::class, RhythmRoute::class, CoachRoute::class)
            .firstOrNull { entry.destination.hasRoute(it) }
    }

    @Test
    fun coachOpener_pushesOneCoachOverTheCurrentScreen() {
        setUpGraph()
        navigate { navigateToTopLevel(TopLevelDestination.RHYTHM) }

        navigate { coachOpener()!!.invoke() }
        navigate { coachOpener()!!.invoke() }

        assertEquals(listOf(TodayRoute::class, RhythmRoute::class, CoachRoute::class), backStack())
    }

    @Test
    fun backFromCoach_returnsToWhereItOpened() {
        setUpGraph()
        navigate { navigateToTopLevel(TopLevelDestination.RHYTHM) }
        navigate { coachOpener()!!.invoke() }

        navigate { popBackStack() }

        assertEquals(listOf(TodayRoute::class, RhythmRoute::class), backStack())
    }
}
