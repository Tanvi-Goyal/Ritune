package com.ritune.feature.today.ui.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ritune.feature.today.ui.screens.TodayScreen
import com.ritune.feature.today.ui.screens.TodayViewModel
import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute

fun NavGraphBuilder.todayScreen(
    onOpenSettings: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    composable<TodayRoute> {
        val viewModel: TodayViewModel = hiltViewModel()
        TodayScreen(
            viewModel = viewModel,
            onOpenSettings = onOpenSettings,
            onOpenPlayer = onOpenPlayer,
        )
    }
}