# Navigation (`:app`)

Single activity, Navigation Compose with type-safe routes (`@Serializable`). Features declare their routes and graph builders; `:app` is the only place that connects them.

## Principles

1. **Features never import each other.** A feature's graph builder takes lambdas for anything outside itself; `:app` wires them to the `NavController`.
2. **Routes are types, not strings.** `@Serializable` objects and classes; arguments are constructor parameters.
3. **All navigation commands live in `NavigationCommands.kt`.** Back stack behaviour is defined once and tested.
4. **Sheets and dialogs are screen state, not routes.** They need the parent's data and dismiss without back stack rules.
5. **Results travel through the database, not the back stack.** The editor saves and pops; the list updates because it observes a Flow.
6. **The player is a view, not an owner.** Starting a routine is a command (`PlayerController.start`) issued first; navigation follows. A player route that opens onto an idle engine closes itself.
7. **Deep links navigate and never act.** No deep link logs an anchor, starts a routine, or changes data. Actions use explicit commands.
8. **No external intent filters.** Notifications use explicit, immutable PendingIntents to `MainActivity`, so no other app gets a link surface into Ritune.

## Routes

| Route | Kind | Feature | Bottom bar | Deep link |
|---|---|---|---|---|
| `OnboardingRoute` | Start (first launch) | onboarding | No | none |
| `TodayRoute` | Tab, bottom of stack | today | Yes | `ritune://today` |
| `RhythmRoute` | Tab | rhythm | Yes | none |
| `RoutinesGraph` > `RoutinesRoute` | Tab | routines | Yes | none |
| `RoutineDetailRoute(routineId)` | Pushed | routines | No | none |
| `RoutineEditorRoute(routineId?)` | Pushed (null = new) | routines | No | none |
| `PlayerRoute` | Pushed, no arguments | player | No | `ritune://player` |
| `SummaryRoute(runId)` | Replaces the player | player | No | `ritune://summary/{runId}` |
| `SettingsGraph` > `SettingsRoute` | Pushed (gear icon) | settings | No | none |
| `RemindersRoute` | Pushed from Settings | settings | No | none |

## Back stack rules

- Today is always the bottom entry. Switching tabs pops to Today with `saveState`, so each tab keeps its place. Back from another tab goes to Today, and Back from Today exits.
- Starting a routine pushes the player. Back leaves the screen but the routine keeps running; the foreground notification stays and Today can offer a way back.
- A finished run replaces the player with the summary. Done or Back returns to where the run started.
- Finishing onboarding pops it, so Back never returns to it.

## Key flows

**Start a routine:** row or detail button, ViewModel calls `PlayerController.start(id)`, on `Started` or `AlreadyRunning` it emits an open-player effect, `:app` runs `navigateToPlayer()`.

**Run finishes while on the player:** player observes `Finished`, calls `onFinished(runId)`, `:app` runs `navigateToSummary(runId)`.

**Reminder or routine notification tap:** explicit PendingIntent with a `ritune://` data URI, `MainActivity` (singleTop) receives it, `handleDeepLink` navigates. The synthetic back stack puts Today underneath.

**Notification actions (Log, Start routine, Pause, Skip):** go to a broadcast receiver or the service, never through navigation.

## Gradle (use current stable versions)

```kotlin
// each :feature:* and :app
plugins { id("org.jetbrains.kotlin.plugin.serialization") }
dependencies {
    implementation(libs.androidx.navigation.compose)        // 2.8 or newer for type-safe routes
    implementation(libs.kotlinx.serialization.json)
}
// :app only
dependencies {
    implementation(libs.androidx.core.splashscreen)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
// feature modules also depend on :core:designsystem and :core:domain; never on each other
```

Manifest: `android:launchMode="singleTop"` on `MainActivity`, `android:enableOnBackInvokedCallback="true"` on the application for predictive back. Provide `AppStartViewModel` through your DI container.

## Adding a screen

1. Add a `@Serializable` route in the feature module, and a builder function that takes lambdas for outside navigation.
2. Add a command in `NavigationCommands.kt` if it needs special back stack behaviour, and a test for it.
3. Register the builder in `RituneNavHost`, connecting the lambdas.
4. Decide whether the bottom bar shows (tab roots only) and update `RituneApp` if it is a new tab.
5. If a notification opens it, add a `DeepLinks` constant and a `navDeepLink`, and make sure opening it changes nothing.