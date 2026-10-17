package com.ritune.feature.rhythm.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ritune.feature.rhythm.ui.screens.RhythmScreen
import com.ritune.feature.rhythm.ui.screens.RhythmViewModel
import kotlinx.serialization.Serializable

@Serializable
data object RhythmRoute

@RequiresApi(Build.VERSION_CODES.O)
fun NavGraphBuilder.rhythmScreen(onOpenSettings: () -> Unit) {
    composable<RhythmRoute> {
        val viewModel: RhythmViewModel = hiltViewModel()
        RhythmScreen(
            viewModel = viewModel,
            onOpenSettings = onOpenSettings,
        )
    }
}