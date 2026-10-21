package com.ritune.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.ritune.core.designsystem.theme.RituneMotion
import com.ritune.core.designsystem.theme.rememberReducedMotion
import com.ritune.core.designsystem.theme.rituneTween
import com.ritune.feature.onboarding.ui.navigation.onboardingScreen
import com.ritune.feature.player.ui.navigation.playerGraph
import com.ritune.feature.rhythm.ui.navigation.rhythmScreen
import com.ritune.feature.routine.ui.navigation.routinesGraph
import com.ritune.feature.settings.ui.navigation.settingsGraph
import com.ritune.feature.today.ui.navigation.todayScreen

@Composable
fun RituneNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier
) {
    val motion = rememberReducedMotion()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(
                rituneTween(
                    motion,
                    RituneMotion.SHORT_MS
                )
            )
        },
        exitTransition = {
            fadeOut(
                rituneTween(
                    motion,
                    RituneMotion.SHORT_MS
                )
            )
        },
    ) {
        onboardingScreen(onFinished = { navController.finishOnboarding() })
        todayScreen(
            onOpenSettings = { navController.navigateToSettings() },
            onOpenPlayer = { navController.navigateToPlayer() },
        )
        rhythmScreen(onOpenSettings = { navController.navigateToSettings() })
        routinesGraph(
            navController = navController,
            onOpenSettings = { navController.navigateToSettings() },
            onOpenPlayer = { navController.navigateToPlayer() },
        )
        playerGraph(
            onFinished = { runId -> navController.navigateToSummary(runId) },
            onClose = { navController.popBackStack() },
            onSummaryDone = { navController.popBackStack() },
        )
        settingsGraph(navController)
    }
}