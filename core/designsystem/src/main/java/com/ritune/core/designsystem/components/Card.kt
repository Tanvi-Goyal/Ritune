package com.ritune.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.ritune.core.designsystem.theme.RituneShapes
import kotlin.math.ceil

private val CardCornerRadius = 16.dp
private val CardBorderWidth = 1.dp

@Composable
fun RituneCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RituneShapes.Card,
        color = colors.surface,
        border = BorderStroke(CardBorderWidth, colors.outlineVariant),
    ) {
        Column(content = content)
    }
}

enum class CardSegment {
    Top, Middle, Bottom, Only;

    companion object {
        fun of(index: Int, count: Int): CardSegment = when {
            count == 1 -> Only
            index == 0 -> Top
            index == count - 1 -> Bottom
            else -> Middle
        }
    }
}

/**
 * One slice of a [RituneCard], for cards whose rows are lazy list items: a lazy list can't wrap its
 * items in one card. Stacked with no gap, the slices draw exactly what one [RituneCard] draws.
 */
@Composable
fun RituneCardSegment(
    segment: CardSegment,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
        Column(
            modifier = modifier
                .cardSegment(segment, colors.surface, colors.outlineVariant)
                .then(
                    segment.shape()
                        ?.let { Modifier.clip(it) } ?: Modifier),
            content = content,
        )
    }
}

private fun CardSegment.shape(): Shape? = when (this) {
    CardSegment.Top -> RoundedCornerShape(
        topStart = CardCornerRadius, topEnd = CardCornerRadius
    )

    CardSegment.Middle -> null
    CardSegment.Bottom -> RoundedCornerShape(
        bottomStart = CardCornerRadius,
        bottomEnd = CardCornerRadius
    )

    CardSegment.Only -> RituneShapes.Card
}

private fun Modifier.cardSegment(
    segment: CardSegment,
    background: Color,
    border: Color
) = drawWithContent {
    val radius = CardCornerRadius.toPx()
    val stroke = ceil(CardBorderWidth.toPx())
    val overflow = radius + stroke
    val top = if (segment == CardSegment.Top || segment == CardSegment.Only) 0f else -overflow
    val bottom = if (segment == CardSegment.Bottom || segment == CardSegment.Only) {
        size.height
    } else {
        size.height + overflow
    }
    clipRect {
        drawRoundRect(
            color = background,
            topLeft = Offset(0f, top),
            size = Size(size.width, bottom - top),
            cornerRadius = CornerRadius(radius),
        )
    }
    drawContent()
    clipRect {
        val half = stroke / 2
        drawRoundRect(
            color = border,
            topLeft = Offset(half, top + half),
            size = Size(size.width - stroke, bottom - top - stroke),
            cornerRadius = CornerRadius(radius - half),
            style = Stroke(stroke),
        )
    }
}

@Composable
fun RituneDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}