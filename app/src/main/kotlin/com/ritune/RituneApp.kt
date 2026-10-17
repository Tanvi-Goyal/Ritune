package com.ritune

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.util.Consumer
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ritune.core.designsystem.components.RituneNavItem
import com.ritune.core.designsystem.components.RituneNavigationBar
import com.ritune.feature.rhythm.ui.navigation.RhythmRoute
import com.ritune.feature.routine.ui.navigation.RoutinesRoute
import com.ritune.feature.today.ui.navigation.TodayRoute
import com.ritune.navigation.RituneNavHost
import com.ritune.navigation.TopLevelDestination
import com.ritune.navigation.navigateToTopLevel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RituneApp(
    startDestination: Any,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val selectedIndex = TopLevelDestination.entries.indexOfFirst { tab ->
        destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
    }

    val showBottomBar = destination != null &&
            (destination.hasRoute<TodayRoute>()
                    || destination.hasRoute<RhythmRoute>()
                    || destination.hasRoute<RoutinesRoute>()
                    )

    // A notification tap while the app is already open arrives as a new intent (launchMode singleTop).
    val activity = LocalContext.current.findActivity()
    DisposableEffect(navController, activity) {
        val listener = Consumer<Intent> { navController.handleDeepLink(it) }
        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }

    val items = TopLevelDestination.entries.map {
        val label = if (it.labelRes != 0) {
            stringResource(it.labelRes)
        } else {
            it.name.lowercase().replaceFirstChar { char -> char.uppercase() }
        }
        RituneNavItem(label, it.iconRes)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                Box(Modifier.testTag("bottomBar")) {
                    RituneNavigationBar(
                        items = items,
                        selectedIndex = selectedIndex,
                        onSelect = {
                            navController.navigateToTopLevel(TopLevelDestination.entries[it])
                        },
                    )
                }
            }
        },
    ) { padding ->
        RituneNavHost(
            navController,
            startDestination,
            Modifier.padding(padding)
        )
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
