# Ritune design system (`:core:designsystem`)

Foundations only: colour, type, shape, spacing, motion, and the theme. Build components on top of these; do not hardcode values.

`design/tokens.json` is the source of truth for designers and Claude Design. `Color.kt` mirrors it. Change both together.

## Use

```kotlin
RituneTheme {                                   // wrap the app and every @Preview
    Text("Evening reset", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    Box(Modifier.padding(Ritune.spacing.lg).background(Ritune.extras.accentMid))
}
```

## Rules

1. Colours come from `MaterialTheme.colorScheme` or `Ritune.extras`. No hex values or `Color(...)` in screens or components.
2. Text styles come from `MaterialTheme.typography` (mapping is in `Type.kt`). No raw `sp` values.
3. Spacing comes from `Ritune.spacing`, shapes from `RituneShapes`.
4. Every `@Preview` renders in light and dark. Build both from the start.
5. Touch targets are at least `Ritune.spacing.minTouchTarget` (48 dp).
6. Animations use `rituneTween(rememberReducedMotion())`.
7. Error colours are reserved for real errors. Never use them for missed anchors or skipped steps.

## Dark theme

Same roles, different values; see the dark theme board on the design canvas and `tokens.json`. The accent lightens in dark mode (`primary` 4DBE9C with dark `onPrimary`), surfaces are near-black green rather than pure black, and the off-switch track (`outline`) is lifted to keep 3:1 against the surface.

## Contrast (computed, WCAG)

| Pair | Needs | Light | Dark |
|---|---|---|---|
| onBackground on background | 4.5:1 | 14.5:1 | 15.7:1 |
| onSurface on surface | 4.5:1 | 15.7:1 | 14.1:1 |
| onSurfaceVariant on background | 4.5:1 | 5.1:1 | 7.5:1 |
| onSurfaceVariant on surface | 4.5:1 | 5.6:1 | 6.7:1 |
| primary on background | 4.5:1 | 5.7:1 | 8.1:1 |
| primary on surface | 4.5:1 | 6.2:1 | 7.3:1 |
| onPrimary on primary | 4.5:1 | 6.2:1 | 7.1:1 |
| onPrimaryContainer on primaryContainer | 4.5:1 | 8.6:1 | 9.8:1 |
| inverseOnSurface on inverseSurface | 4.5:1 | 15.7:1 | 15.7:1 |
| inversePrimary on inverseSurface | 4.5:1 | 7.4:1 | 5.3:1 |
| outline on surface | 3.0:1 | 3.1:1 | 3.4:1 |


## Setup

```kotlin
// core/designsystem/build.gradle.kts (use current stable versions in your catalog)
plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.cadence.designsystem"
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
```

Fonts: add the static Manrope weights (regular, semibold, bold, extrabold) to `src/main/res/font/` as `manrope_regular.ttf`, `manrope_semibold.ttf`, `manrope_bold.ttf`, `manrope_extrabold.ttf`.

## Not here yet

- Components (`AnchorRow`, `ConsistencyRing`, `StepProgress`, `TimerText`, `RituneSwitch`, snackbar). They go in this module, built only from these tokens.
- A token sync test that fails if `tokens.json` and `Color.kt` disagree.
- Material Symbols or custom icon set.