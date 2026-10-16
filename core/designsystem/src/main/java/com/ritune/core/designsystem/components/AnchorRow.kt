package com.ritune.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ritune.core.designsystem.components.SecondaryButton

@Composable
fun AnchorRow(
    name: String,
    timeText: String,
    logged: Boolean,
    statusDescription: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .then(
                    if (onClick != null) Modifier.clickable(
                        onClickLabel = onClickLabel,
                        role = Role.Button,
                        onClick = onClick
                    )
                    else Modifier,
                )
                .semantics(mergeDescendants = true) {
                    stateDescription = statusDescription
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusDot(logged)
            if (logged) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            } else {
                val caption = MaterialTheme.typography.bodySmall.toSpanStyle()
                    .copy(color = colors.onSurfaceVariant)
                Text(
                    text = buildAnnotatedString {
                        append(name)
                        append("  ")
                        withStyle(caption) { append(timeText) }
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (!logged && actionLabel != null && onAction != null) {
            SecondaryButton(text = actionLabel, onClick = onAction, compact = true)
        }
    }
}

@Composable
private fun StatusDot(logged: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(22.dp)
            .then(
                if (logged) Modifier
                    .background(colors.primary, CircleShape)
                else Modifier
                    .border(2.dp, colors.outline, CircleShape),
            )
            .clearAndSetSemantics { },
    )
}