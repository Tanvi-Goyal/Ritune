package com.ritune.feature.today.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute

@RequiresApi(Build.VERSION_CODES.O)
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