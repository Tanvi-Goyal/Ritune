package com.ritune.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Wrap the app (and every @Preview) in this. No dynamic color on purpose:
 * the teal accent is the brand.
 * Pass the user's theme setting (S-2) as [themeMode].
 */
@Composable
fun RituneTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(
        LocalRituneExtras provides if (dark) DarkExtras else LightExtras,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColorScheme else LightColorScheme,
            typography = RituneTypography,
            shapes = RituneMaterialShapes,
            content = content,
        )
    }
}

/** Access to the tokens Material 3 does not cover: Ritune.extras.accentMid, Ritune.spacing.lg. */
object Ritune {
    val extras: RituneExtras
        @Composable @ReadOnlyComposable get() = LocalRituneExtras.current

    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
}