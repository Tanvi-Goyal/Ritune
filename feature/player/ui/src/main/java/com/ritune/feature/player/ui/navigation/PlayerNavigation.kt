package com.ritune.feature.player.ui.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.ritune.core.navigation.DeepLinks
import com.ritune.feature.player.ui.screens.player.PlayerScreen
import com.ritune.feature.player.ui.screens.player.PlayerViewModel
import com.ritune.feature.player.ui.screens.summary.SummaryScreen
import com.ritune.feature.player.ui.screens.summary.SummaryViewModel
import kotlinx.serialization.Serializable

/**
 * The player is a VIEW of the running routine, not its owner. It takes no arguments: starting is a command
 * (StartRoutine) issued before navigating. If it opens while nothing runs, it closes itself.
 */
@Serializable
data object PlayerRoute

@Serializable
data class SummaryRoute(val runId: String)

/** Deep links only navigate. Opening one never starts, stops, or changes a run. */
fun NavGraphBuilder.playerGraph(
    onFinished: (runId: String) -> Unit,
    onClose: () -> Unit,
    onSummaryDone: () -> Unit,
) {
    composable<PlayerRoute>(
        deepLinks = listOf(
            navDeepLink<PlayerRoute>(basePath = DeepLinks.PLAYER)
        )
    ) {
        val viewModel: PlayerViewModel = hiltViewModel()
        PlayerScreen(
            viewModel = viewModel,
            onFinished = onFinished,
            onClose = onClose
        )
    }
    composable<SummaryRoute>(
        deepLinks = listOf(
            navDeepLink<SummaryRoute>(basePath = DeepLinks.SUMMARY)
        )
    ) {
        val viewModel: SummaryViewModel = hiltViewModel()
        SummaryScreen(
            viewModel = viewModel,
            onDone = onSummaryDone
        )
    }
}
