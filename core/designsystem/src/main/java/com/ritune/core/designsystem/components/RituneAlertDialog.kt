package com.ritune.core.designsystem.components


import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.ritune.core.designsystem.theme.RituneShapes

@Composable
fun RituneAlertDialog(
    title: String,
    text: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RituneShapes.Dialog,
        containerColor = colors.surface,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
        },
        text = {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RituneShapes.Button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.inverseSurface,
                    contentColor = colors.inverseOnSurface
                ),
            ) { Text(confirmLabel, style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    dismissLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )
            }
        },
    )
}