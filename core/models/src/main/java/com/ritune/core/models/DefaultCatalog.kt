package com.ritune.core.models

import kotlinx.datetime.LocalTime
import kotlin.time.Duration.Companion.minutes

object DefaultCatalog {
    val wake = Anchor(
        AnchorId("wake"),
        "Wake",
        LocalTime(7, 0)
    )
    val daylight = Anchor(
        AnchorId("daylight"),
        "Daylight",
        LocalTime(7, 15)
    )
    val workout = Anchor(
        AnchorId("workout"),
        "Workout",
        LocalTime(12, 30)
    )
    val lastMeal = Anchor(
        AnchorId("last-meal"),
        "Last meal",
        LocalTime(19, 30)
    )
    val shutdown = Anchor(
        AnchorId("shutdown"),
        "Shutdown",
        LocalTime(22, 0)
    )

    val anchors = listOf(wake, daylight, workout, lastMeal, shutdown)

    private val changeCue = Cue.Haptic(HapticPattern.STEP_CHANGE)

    val eveningReset = Routine(
        id = RoutineId("evening-reset"),
        name = "Evening reset",
        anchorId = shutdown.id,
        steps = listOf(
            Step(
                StepId("screens-away"),
                "Put screens away",
                2.minutes,
                "Close laptops and tablets.",
                listOf(changeCue, Cue.Speech("Put screens away"))
            ),
            Step(
                StepId("top-three"),
                "Write tomorrow's top three",
                3.minutes,
                null,
                listOf(changeCue, Cue.Speech("Write tomorrow's top three"))
            ),
            Step(
                StepId("stretch"),
                "Light stretch",
                2.minutes,
                null,
                listOf(changeCue, Cue.Speech("Light stretch"))
            ),
            Step(
                StepId("breathe"),
                "Slow breathing",
                1.minutes,
                null, listOf(changeCue, Cue.Speech("Slow breathing"))
            ),
        ),
    )
}