package com.ritune.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import kotlin.time.Duration

@Composable
fun TimerText(
    remaining: Duration,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    contentDescription: String? = null,
) {
    Text(
        text = formatTimer(remaining),
        modifier = if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else {
            modifier
        },
        color = color,
        style = style.copy(fontFeatureSettings = "tnum"),
        maxLines = 1,
        softWrap = false,
    )
}

fun formatTimer(remaining: Duration): String {
    val totalSeconds = (remaining.inWholeMilliseconds
        .coerceAtLeast(0) + 999) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}