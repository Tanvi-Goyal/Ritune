package com.ritune.feature.routine.ui.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.ritune.feature.routine.ui.navigation.screens.routinedetail.RoutineDetailScreen
import com.ritune.feature.routine.ui.navigation.screens.routinedetail.RoutineDetailViewModel
import com.ritune.feature.routine.ui.navigation.screens.routineeditor.RoutineEditorScreen
import com.ritune.feature.routine.ui.navigation.screens.routineeditor.RoutineEditorViewModel
import com.ritune.feature.routine.ui.navigation.screens.routineslist.RoutinesListScreen
import com.ritune.feature.routine.ui.navigation.screens.routineslist.RoutinesListViewModel
import kotlinx.serialization.Serializable

/** The Routines tab. Navigating to this graph restores whatever the user had open inside it. */
@Serializable
data object RoutinesGraph

@Serializable
data object RoutinesRoute

@Serializable
data class RoutineDetailRoute(val routineId: String)

/** routineId = null means "new routine". */
@Serializable
data class RoutineEditorRoute(val routineId: String? = null)

/** Internal moves use [navController]; moves into other features arrive as lambdas. */
fun NavGraphBuilder.routinesGraph(
    navController: NavController,
    onOpenSettings: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    navigation<RoutinesGraph>(startDestination = RoutinesRoute) {
        composable<RoutinesRoute> {
            val viewModel: RoutinesListViewModel = hiltViewModel()
            RoutinesListScreen(
                viewModel = viewModel,
                onOpenRoutine = { id ->
                    navController.navigate(
                        RoutineDetailRoute(id)
                    )
                },
                onNewRoutine = {
                    navController.navigate(RoutineEditorRoute())
                },
                onOpenSettings = onOpenSettings,
                onOpenPlayer = onOpenPlayer,
            )
        }

        composable<RoutineDetailRoute> { entry ->
            val viewModel: RoutineDetailViewModel = hiltViewModel()
            RoutineDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { id ->
                    navController.navigate(RoutineEditorRoute(id))
                },
                onOpenPlayer = onOpenPlayer,
            )
        }

        composable<RoutineEditorRoute> {
            val viewModel: RoutineEditorViewModel = hiltViewModel()
            RoutineEditorScreen(
                viewModel = viewModel,
                onFinished = { navController.popBackStack() },
            )
        }
    }
}