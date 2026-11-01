package com.ritune.core.designsystem

/**
 * Test tags shared by the app, its UI tests and the Macrobenchmark journeys.
 * RituneApp exposes them as resource ids, so UiAutomator finds them with `By.res(tag)`.
 * Renaming one breaks the tests that look it up.
 */
object TestTags {
    const val BOTTOM_BAR = "bottomBar"
    const val TODAY_CONTENT = "todayContent"
    const val ROUTINES_LIST = "routinesList"
    const val RHYTHM_CONTENT = "rhythmContent"
    const val NAV_TODAY = "navToday"
    const val NAV_RHYTHM = "navRhythm"
    const val NAV_ROUTINES = "navRoutines"
}
