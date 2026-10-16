package com.ritune.feature.onboarding.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ritune.core.designsystem.components.PlaceholderAction
import com.ritune.core.designsystem.components.PlaceholderScreen
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute


fun NavGraphBuilder.onboardingScreen(onFinished: () -> Unit) {
    composable<OnboardingRoute> {
        PlaceholderScreen(
            "Onboarding",
            actions = listOf(
                PlaceholderAction("Get started", onFinished)
            )
        )
    }
}