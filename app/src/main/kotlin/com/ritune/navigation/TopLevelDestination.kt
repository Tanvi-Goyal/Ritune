package com.ritune.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.TestTags
import com.ritune.core.designsystem.TraceSections
import com.ritune.feature.rhythm.ui.navigation.RhythmRoute
import com.ritune.feature.routine.ui.navigation.RoutinesGraph
import com.ritune.feature.routine.ui.navigation.RoutinesRoute
import com.ritune.feature.today.ui.navigation.TodayRoute
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val testTag: String,
    val openTraceSection: String? = null,
) {
    TODAY(
        TodayRoute,
        TodayRoute::class,
        labelRes = R.string.nav_today,
        iconRes = R.drawable.ic_nav_today,
        testTag = TestTags.NAV_TODAY,
    ),
    RHYTHM(
        RhythmRoute,
        RhythmRoute::class,
        labelRes = R.string.nav_rhythm,
        iconRes = R.drawable.ic_nav_rhythm,
        testTag = TestTags.NAV_RHYTHM,
    ),
    ROUTINES(
        RoutinesGraph,
        RoutinesRoute::class,
        labelRes = R.string.nav_routines,
        iconRes = R.drawable.ic_nav_routines,
        testTag = TestTags.NAV_ROUTINES,
        openTraceSection = TraceSections.ROUTINES_OPEN,
    )
}
