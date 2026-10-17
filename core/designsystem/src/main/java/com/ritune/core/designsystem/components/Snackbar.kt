package com.ritune.core.designsystem.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun RituneSnackbar(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    if (actionLabel == null) {
        Snackbar(
            modifier = modifier,
            shape = shape,
            containerColor = colors.inverseSurface,
            contentColor = colors.inverseOnSurface,
        ) { Text(message) }
    } else {
        Snackbar(
            modifier = modifier,
            action = {
                TextButton(onClick = onAction) {
                    Text(
                        actionLabel,
                        color = colors.inversePrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            shape = shape,
            containerColor = colors.inverseSurface,
            contentColor = colors.inverseOnSurface,
        ) { Text(message) }
    }
}

@Composable
fun RituneSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        RituneSnackbar(
            message = data.visuals.message,
            actionLabel = data.visuals.actionLabel,
            onAction = { data.performAction() },
        )
    }
}

suspend fun SnackbarHostState.showUndo(
    message: String,
    actionLabel: String,
    timeoutMs: Long = 5_000L,
): Boolean = coroutineScope {
    val dismisser = launch {
        delay(timeoutMs.milliseconds)
        currentSnackbarData?.dismiss()
    }
    val result = showSnackbar(
        message = message,
        actionLabel = actionLabel,
        withDismissAction = false,
        duration = SnackbarDuration.Indefinite,
    )
    dismisser.cancel()
    result == SnackbarResult.ActionPerformed
}