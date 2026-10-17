package com.ritune.core.designsystem.components


import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ritune.core.designsystem.theme.rememberReducedMotion
import com.ritune.core.designsystem.theme.rituneTween

@Composable
fun ConsistencyRing(
    done: Int,
    total: Int,
    description: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 84.dp,
    strokeWidth: Dp = 8.dp,
) {
    val colors = MaterialTheme.colorScheme
    val fraction = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec = rituneTween(rememberReducedMotion()),
        label = "ringProgress",
    )
    Box(
        modifier = modifier
            .size(diameter)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val track = colors.outlineVariant
        val progress = colors.primary
        Canvas(Modifier.matchParentSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2f
            val topLeft = Offset(inset, inset)
            val arcSize = Size(
                width = this.size.width - stroke.width,
                height = this.size.height - stroke.width
            )
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            if (animated > 0f) {
                drawArc(
                    color = progress,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
        }
        Text(
            text = "$done/$total",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.onSurface,
        )
    }
}