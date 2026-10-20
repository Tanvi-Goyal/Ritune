package com.ritune.feature.settings.ui.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.ritune.core.designsystem.components.PlaceholderAction
import com.ritune.core.designsystem.components.PlaceholderScreen
import com.ritune.feature.settings.ui.screens.SettingsScreen
import com.ritune.feature.settings.ui.screens.SettingsViewModel
import kotlinx.serialization.Serializable

@Serializable
data object SettingsGraph

@Serializable
data object SettingsRoute

@Serializable
data object RemindersRoute

fun NavGraphBuilder.settingsGraph(navController: NavController) {
    navigation<SettingsGraph>(startDestination = SettingsRoute) {
        composable<SettingsRoute> {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenReminders = { navController.navigate(RemindersRoute) },
            )
        }
        composable<RemindersRoute> {
            PlaceholderScreen(
                title = "Reminders",
                actions = listOf(PlaceholderAction("Back") { navController.popBackStack() }),
            )
        }
    }
}