package com.ritune.core.models

import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * What a fresh install (and "Delete all data") starts with: one day built around circadian anchors,
 * from a steady wake time to a steady bedtime, with a short guided routine for each.
 * Anchor ids are stable: logged events and the Rhythm screen refer to them.
 */
object DefaultCatalog {
    val wake = Anchor(AnchorId("wake"), "Wake", LocalTime(7, 0))
    val daylight = Anchor(AnchorId("daylight"), "Morning light", LocalTime(7, 30))
    val workout = Anchor(AnchorId("workout"), "Move", LocalTime(17, 30))
    val lastMeal = Anchor(AnchorId("last-meal"), "Last meal", LocalTime(19, 30))
    val shutdown = Anchor(AnchorId("shutdown"), "Screens off", LocalTime(21, 30))
    val lightsOut = Anchor(AnchorId("lights-out"), "Lights out", LocalTime(22, 30))

    val anchors = listOf(wake, daylight, workout, lastMeal, shutdown, lightsOut)

    val morningStart = Routine(
        id = RoutineId("morning-start"),
        name = "Morning start",
        anchorId = wake.id,
        steps = listOf(
            step("water", "Drink a glass of water", 1.minutes, "Rehydrate before coffee."),
            step("bed", "Make your bed", 2.minutes, "A first small win."),
            step("mobility", "Wake up your body", 5.minutes, "Neck, shoulders, hips, then a slow forward fold."),
            step("plan", "Pick today's top three", 2.minutes, "What would make today a good day?"),
        ),
        builtIn = true,
    )

    val lightWalk = Routine(
        id = RoutineId("light-walk"),
        name = "Morning light walk",
        anchorId = daylight.id,
        steps = listOf(
            step("outside", "Head outside", 1.minutes, "No sunglasses if it's safe. Daylight sets your body clock."),
            step("walk", "Easy walk", 12.minutes, "Relaxed pace. Look toward the horizon, not your phone."),
            step("breathe", "Slow breathing", 2.minutes, "In for four, out for six."),
        ),
        builtIn = true,
    )

    val quickWorkout = Routine(
        id = RoutineId("quick-workout"),
        name = "20-minute strength",
        anchorId = workout.id,
        steps = listOf(
            step("warm-up", "Warm up", 3.minutes, "March in place, arm circles, hip circles."),
            step("squats", "Squats", 2.minutes, "Steady pace. Sit back, chest up."),
            step("push-ups", "Push-ups", 2.minutes, "From the knees is fine. Quality over count."),
            step("rest-1", "Rest", 1.minutes),
            step("lunges", "Lunges", 2.minutes, "Alternate legs."),
            step("plank", "Plank", 1.minutes, "Rest when your form breaks, then continue."),
            step("rest-2", "Rest", 1.minutes),
            step("glute-bridges", "Glute bridges", 2.minutes),
            step("dead-bugs", "Dead bugs", 2.minutes, "Slow and controlled. Lower back stays down."),
            step("cool-down", "Cool down stretch", 4.minutes, "Hamstrings, quads, chest, shoulders."),
        ),
        builtIn = true,
    )

    val kitchenClose = Routine(
        id = RoutineId("kitchen-close"),
        name = "Close the kitchen",
        anchorId = lastMeal.id,
        steps = listOf(
            step("put-away", "Put food away", 2.minutes, "Out of sight helps late snacking stay out of mind."),
            step("water", "Fill tomorrow's water bottle", 1.minutes),
            step("teeth", "Brush your teeth", 2.minutes, "A clear signal that eating is done for the day."),
        ),
        builtIn = true,
    )

    val eveningReset = Routine(
        id = RoutineId("evening-reset"),
        name = "Evening reset",
        anchorId = shutdown.id,
        steps = listOf(
            step("screens-away", "Put screens away", 2.minutes, "Close laptops and tablets. Phone on the charger, outside the bedroom."),
            step("top-three", "Write tomorrow's top three", 3.minutes),
            step("tidy", "Ten-minute tidy", 10.minutes, "Reset one room so tomorrow starts clear."),
            step("lights", "Dim the lights", 1.minutes, "Lamps instead of overhead lights."),
        ),
        builtIn = true,
    )

    val windDown = Routine(
        id = RoutineId("wind-down"),
        name = "Wind down",
        // Not linked: starting a routine logs its anchor, and lights out comes after winding down.
        steps = listOf(
            step("stretch", "Light stretch", 3.minutes, "Gentle and slow. Nothing that wakes you up."),
            step("read", "Read on paper", 10.minutes),
            step("breathe", "4-7-8 breathing", 2.minutes, "In for four, hold for seven, out for eight."),
        ),
        builtIn = true,
    )

    val focusBlock = Routine(
        id = RoutineId("focus-block"),
        name = "Focus block",
        steps = listOf(
            step("clear", "Clear your desk", 2.minutes, "Close extra tabs. Phone face down, out of reach."),
            step("intent", "Name one task", 1.minutes, "Only one."),
            step("focus", "Deep work", 25.minutes, "If a thought pulls you away, write it down and return."),
            step("break", "Stand up and move", 5.minutes, "Look at something far away to rest your eyes."),
        ),
        builtIn = true,
    )

    val routines = listOf(
        morningStart,
        lightWalk,
        quickWorkout,
        kitchenClose,
        eveningReset,
        windDown,
        focusBlock,
    )

    private fun step(
        id: String,
        title: String,
        duration: Duration,
        instruction: String? = null,
    ) = Step(StepId(id), title, duration, instruction, defaultStepCues(title))
}
