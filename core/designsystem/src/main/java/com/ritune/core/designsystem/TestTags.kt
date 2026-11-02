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
    const val ROUTINE_DETAIL_CONTENT = "routineDetailContent"
    const val ROUTINE_START = "routineStart"
    const val ROUTINE_EDIT = "routineEdit"
    const val ROUTINE_EDITOR_CONTENT = "routineEditorContent"
    const val PLAYER_CONTENT = "playerContent"
    const val PLAYER_SKIP = "playerSkip"
    const val NAV_TODAY = "navToday"
    const val NAV_RHYTHM = "navRhythm"
    const val NAV_ROUTINES = "navRoutines"

    /** Seed data, not a tag: the routine the benchmark seeds and opens, found by its name. */
    const val LONG_ROUTINE_NAME = "Benchmark long routine"
    const val LONG_ROUTINE_STEPS = 15
}
