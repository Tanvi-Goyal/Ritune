package com.ritune.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ritune.feature.rhythm.ui.navigation.RhythmRoute
import com.ritune.feature.routine.ui.navigation.RoutinesRoute
import com.ritune.feature.today.ui.navigation.TodayRoute
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val labelRes: Int = 0,
    @DrawableRes val iconRes: Int = 0
) {
    TODAY(
        TodayRoute,
        TodayRoute::class,
        iconRes = android.R.drawable.ic_menu_today
    ),
    RHYTHM(
        RhythmRoute,
        RhythmRoute::class,
        iconRes = android.R.drawable.ic_menu_compass
    ),
    ROUTINES(
        RoutinesRoute,
        RoutinesRoute::class,
        iconRes = android.R.drawable.ic_menu_agenda
    )
}
