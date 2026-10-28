package com.ritune.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.feature.onboarding.ui.navigation.OnboardingRoute
import com.ritune.feature.player.ui.navigation.PlayerRoute
import com.ritune.feature.player.ui.navigation.SummaryRoute
import com.ritune.feature.rhythm.ui.navigation.RhythmRoute
import com.ritune.feature.routine.ui.navigation.RoutinesGraph
import com.ritune.feature.routine.ui.navigation.RoutinesRoute
import com.ritune.feature.settings.ui.navigation.SettingsGraph
import com.ritune.feature.settings.ui.navigation.SettingsRoute
import com.ritune.feature.today.ui.navigation.TodayRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KClass
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class NavigationCommandsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController

    private fun setUpGraph(start: Any = TodayRoute) {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            NavHost(navController, startDestination = start) {
                composable<OnboardingRoute> {}
                composable<TodayRoute> {}
                composable<RhythmRoute> {}
                navigation<RoutinesGraph>(startDestination = RoutinesRoute) {
                    composable<RoutinesRoute> {}
                }
                composable<PlayerRoute> {}
                composable<SummaryRoute> {}
                navigation<SettingsGraph>(startDestination = SettingsRoute) {
                    composable<SettingsRoute> {}
                }
            }
        }
    }

    private fun navigate(command: TestNavHostController.() -> Unit) {
        composeTestRule.runOnIdle { navController.command() }
        composeTestRule.waitForIdle()
    }

    private fun backStack(): List<KClass<*>> = navController.currentBackStack.value
        .mapNotNull { entry ->
            ROUTES.firstOrNull { entry.destination.hasRoute(it) }
        }

    @Test
    fun navigateToTopLevel_switchingTabs_keepsTodayAtTheBottom() {
        setUpGraph()

        navigate { navigateToTopLevel(TopLevelDestination.RHYTHM) }
        navigate { navigateToTopLevel(TopLevelDestination.ROUTINES) }

        assertEquals(listOf(TodayRoute::class, RoutinesRoute::class), backStack())
    }

    @Test
    fun navigateToTopLevel_returningToRhythm_restoresItsState() {
        setUpGraph()
        navigate { navigateToTopLevel(TopLevelDestination.RHYTHM) }
        val rhythm = navController.currentBackStackEntry!!.id

        navigate { navigateToTopLevel(TopLevelDestination.TODAY) }
        navigate { navigateToTopLevel(TopLevelDestination.RHYTHM) }

        assertEquals(rhythm, navController.currentBackStackEntry!!.id)
    }

    @Test
    fun navigateToTopLevel_returningToRoutines_restoresItsState() {
        setUpGraph()
        navigate { navigateToTopLevel(TopLevelDestination.ROUTINES) }
        val routines = navController.currentBackStackEntry!!.id

        navigate { navigateToTopLevel(TopLevelDestination.TODAY) }
        navigate { navigateToTopLevel(TopLevelDestination.ROUTINES) }

        assertEquals(routines, navController.currentBackStackEntry!!.id)
    }

    @Test
    fun navigateToTopLevel_currentTab_doesNotStackACopy() {
        setUpGraph()

        navigate { navigateToTopLevel(TopLevelDestination.RHYTHM) }
        navigate { navigateToTopLevel(TopLevelDestination.RHYTHM) }

        assertEquals(listOf(TodayRoute::class, RhythmRoute::class), backStack())
    }

    @Test
    fun navigateToPlayerAndSettings_twice_openOneCopyEach() {
        setUpGraph()

        navigate { navigateToSettings() }
        navigate { navigateToSettings() }
        assertEquals(listOf(TodayRoute::class, SettingsRoute::class), backStack())

        navigate { navigateToPlayer() }
        navigate { navigateToPlayer() }
        assertEquals(listOf(TodayRoute::class, SettingsRoute::class, PlayerRoute::class), backStack())
    }

    @Test
    fun navigateToSummary_replacesThePlayer() {
        setUpGraph()
        navigate { navigateToTopLevel(TopLevelDestination.ROUTINES) }
        navigate { navigateToPlayer() }

        navigate { navigateToSummary("run-7") }

        assertEquals(listOf(TodayRoute::class, RoutinesRoute::class, SummaryRoute::class), backStack())
        assertEquals("run-7", navController.currentBackStackEntry!!.toRoute<SummaryRoute>().runId)
    }

    @Test
    fun finishOnboarding_removesOnboardingFromTheBackStack() {
        setUpGraph(start = OnboardingRoute)

        navigate { finishOnboarding() }

        assertEquals(listOf(TodayRoute::class), backStack())
    }

    private companion object {
        val ROUTES = listOf(
            OnboardingRoute::class,
            TodayRoute::class,
            RhythmRoute::class,
            RoutinesRoute::class,
            PlayerRoute::class,
            SummaryRoute::class,
            SettingsRoute::class,
        )
    }
}
