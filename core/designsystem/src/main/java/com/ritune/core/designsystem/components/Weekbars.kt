package com.ritune.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ritune.core.designsystem.theme.Ritune

@Composable
fun WeekBars(
    values: List<Float?>,
    labels: List<String>,
    descriptions: List<String>,
    modifier: Modifier = Modifier,
    highlightIndex: Int? = null,
    barAreaHeight: Dp = 120.dp,
) {
    require(values.size == labels.size && values.size == descriptions.size) {
        "values, labels and descriptions must have the same size"
    }
    val colors = MaterialTheme.colorScheme
    val extras = Ritune.extras
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEachIndexed { index, value ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = descriptions[index]
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(barAreaHeight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val barHeight = if (value == null) 4.dp
                    else (barAreaHeight * value.coerceIn(0f, 1f))
                        .coerceAtLeast(4.dp)
                    val color = when {
                        value == null -> colors.outlineVariant
                        index == highlightIndex -> colors.primary
                        else -> extras.accentMid
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .background(color, RoundedCornerShape(6.dp))
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    labels[index],
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}