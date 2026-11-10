package com.ritune.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

@Suppress("UnusedReceiverParameter")
internal fun NavGraphBuilder.coachDestination(navController: NavController) = Unit

@Suppress("UnusedReceiverParameter")
internal fun NavController.coachOpener(): (() -> Unit)? = null
