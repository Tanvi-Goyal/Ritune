package com.ritune.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import com.ritune.core.designsystem.R

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

private fun style(size: Int, weight: FontWeight, line: Int, tracking: Int = 0) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

internal val RituneTypography = Typography(
    displayLarge = style(96, FontWeight.ExtraBold, 100, tracking = -2),
    displayMedium = style(44, FontWeight.ExtraBold, 48),
    headlineLarge = style(30, FontWeight.ExtraBold, 34),
    titleMedium = style(18, FontWeight.Bold, 24),
    titleSmall = style(15, FontWeight.SemiBold, 20),
    bodyLarge = style(15, FontWeight.Normal, 22),
    bodyMedium = style(15, FontWeight.Normal, 22),
    bodySmall = style(13, FontWeight.Normal, 18),
    labelLarge = style(16, FontWeight.Bold, 20),
    labelMedium = style(13, FontWeight.SemiBold, 18),
    labelSmall = style(11, FontWeight.SemiBold, 14),
)