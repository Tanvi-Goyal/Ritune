package com.ritune.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ritune.feature.onboarding.ui.navigation.OnboardingRoute
import com.ritune.feature.player.ui.navigation.PlayerRoute
import com.ritune.feature.player.ui.navigation.SummaryRoute
import com.ritune.feature.settings.ui.navigation.SettingsGraph
import com.ritune.feature.today.ui.navigation.TodayRoute

fun NavController.navigateToTopLevel(topLevelDestination: TopLevelDestination) {
    navigate(topLevelDestination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.navigateToSettings() {
    navigate(SettingsGraph) { launchSingleTop = true }
}

fun NavController.navigateToPlayer() {
    navigate(PlayerRoute) { launchSingleTop = true }
}

fun NavController.navigateToSummary(runId: String) {
    navigate(SummaryRoute(runId)) {
        popUpTo<PlayerRoute> { inclusive = true }
        launchSingleTop = true
    }
}

fun NavController.finishOnboarding() {
    navigate(TodayRoute) {
        popUpTo<OnboardingRoute> { inclusive = true }
        launchSingleTop = true
    }
}
