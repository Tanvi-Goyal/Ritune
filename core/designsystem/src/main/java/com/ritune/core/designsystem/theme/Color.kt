package com.ritune.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class RituneExtras(
    val accentMid: Color,
)

internal val LightExtras = RituneExtras(accentMid = Color(0xFF7DBFA9))
internal val DarkExtras = RituneExtras(accentMid = Color(0xFF2F8F75))
internal val LocalRituneExtras = staticCompositionLocalOf { LightExtras }

internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF0F6E56),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDEFE8),
    onPrimaryContainer = Color(0xFF0B4A3A),
    background = Color(0xFFF7F6F2),
    onBackground = Color(0xFF1C2523),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C2523),
    surfaceVariant = Color(0xFFF1F0EA),
    onSurfaceVariant = Color(0xFF5E6B67),
    outline = Color(0xFF87958F),
    outlineVariant = Color(0xFFE4E2DB),
    inverseSurface = Color(0xFF1C2523),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF7DBFA9),
    scrim = Color(0xFF1C2523),
    secondary = Color(0xFF0F6E56),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDEFE8),
    onSecondaryContainer = Color(0xFF0B4A3A),
    tertiary = Color(0xFF0F6E56),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDEFE8),
    onTertiaryContainer = Color(0xFF0B4A3A),
    surfaceTint = Color(0xFF0F6E56),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFF7F6F2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F6F2),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFF1F0EA),
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF4DBE9C),
    onPrimary = Color(0xFF06251C),
    primaryContainer = Color(0xFF17382F),
    onPrimaryContainer = Color(0xFFBFEBDD),
    background = Color(0xFF0F1513),
    onBackground = Color(0xFFE8EEEC),
    surface = Color(0xFF18201D),
    onSurface = Color(0xFFE8EEEC),
    surfaceVariant = Color(0xFF222C28),
    onSurfaceVariant = Color(0xFF9AA8A3),
    outline = Color(0xFF66746F),
    outlineVariant = Color(0xFF2A3531),
    inverseSurface = Color(0xFFE8EEEC),
    inverseOnSurface = Color(0xFF0F1513),
    inversePrimary = Color(0xFF0F6E56),
    scrim = Color(0xFF000000),
    secondary = Color(0xFF4DBE9C),
    onSecondary = Color(0xFF06251C),
    secondaryContainer = Color(0xFF17382F),
    onSecondaryContainer = Color(0xFFBFEBDD),
    tertiary = Color(0xFF4DBE9C),
    onTertiary = Color(0xFF06251C),
    tertiaryContainer = Color(0xFF17382F),
    onTertiaryContainer = Color(0xFFBFEBDD),
    surfaceTint = Color(0xFF4DBE9C),
    surfaceBright = Color(0xFF18201D),
    surfaceDim = Color(0xFF0F1513),
    surfaceContainerLowest = Color(0xFF18201D),
    surfaceContainerLow = Color(0xFF0F1513),
    surfaceContainer = Color(0xFF18201D),
    surfaceContainerHigh = Color(0xFF18201D),
    surfaceContainerHighest = Color(0xFF222C28),
)