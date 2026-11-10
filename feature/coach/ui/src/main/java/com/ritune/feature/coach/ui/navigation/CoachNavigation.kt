package com.ritune.feature.coach.ui.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ritune.feature.coach.ui.screens.CoachScreen
import com.ritune.feature.coach.ui.screens.CoachViewModel
import kotlinx.serialization.Serializable

@Serializable
data object CoachRoute

fun NavGraphBuilder.coachScreen(onBack: () -> Unit) {
    composable<CoachRoute> {
        val viewModel: CoachViewModel = hiltViewModel()
        CoachScreen(viewModel = viewModel, onBack = onBack)
    }
}
