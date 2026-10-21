package com.ritune.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ritune.core.designsystem.R
import com.ritune.feature.rhythm.ui.navigation.RhythmRoute
import com.ritune.feature.routine.ui.navigation.RoutinesRoute
import com.ritune.feature.today.ui.navigation.TodayRoute
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    TODAY(
        TodayRoute,
        TodayRoute::class,
        labelRes = R.string.nav_today,
        iconRes = R.drawable.ic_nav_today,
    ),
    RHYTHM(
        RhythmRoute,
        RhythmRoute::class,
        labelRes = R.string.nav_rhythm,
        iconRes = R.drawable.ic_nav_rhythm,
    ),
    ROUTINES(
        RoutinesRoute,
        RoutinesRoute::class,
        labelRes = R.string.nav_routines,
        iconRes = R.drawable.ic_nav_routines,
    )
}
