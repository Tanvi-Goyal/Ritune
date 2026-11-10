package com.ritune.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.ritune.feature.coach.ui.navigation.CoachRoute
import com.ritune.feature.coach.ui.navigation.coachScreen

internal fun NavGraphBuilder.coachDestination(navController: NavController) {
    coachScreen(onBack = { navController.popBackStack() })
}

internal fun NavController.coachOpener(): (() -> Unit)? = { navigateToCoach() }

internal fun NavController.navigateToCoach() {
    navigate(CoachRoute) { launchSingleTop = true }
}
