package com.ritune.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Named shapes for components. Sheets use SheetTop explicitly; Material's default is 28dp. */
object RituneShapes {
    val Card = RoundedCornerShape(16.dp)
    val Button = RoundedCornerShape(percent = 50)
    val SheetTop = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val Dialog = RoundedCornerShape(28.dp)
    val Switch = RoundedCornerShape(14.dp)
}

internal val RituneMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RituneShapes.Card,
    large = RoundedCornerShape(24.dp),
    extraLarge = RituneShapes.Dialog,
)