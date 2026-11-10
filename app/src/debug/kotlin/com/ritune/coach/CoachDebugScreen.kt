package com.ritune.coach

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritune.R
import com.ritune.core.models.coach.CoachError

@Composable
fun CoachDebugScreen(
    viewModel: CoachDebugViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CoachDebugContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
fun CoachDebugContent(
    state: CoachDebugUiState,
    onEvent: (CoachDebugEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.coach_debug_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onEvent(CoachDebugEvent.NewConversation) }) {
                Text(stringResource(R.string.coach_debug_new_conversation))
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages) { message ->
                Text(
                    text = message.text,
                    fontWeight = if (message.fromUser) FontWeight.SemiBold else null,
                )
            }
        }

        state.runningTool?.let {
            Text(
                text = stringResource(R.string.coach_debug_tool_running, it),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        state.lastTurn?.let { Text(it.text(), style = MaterialTheme.typography.labelMedium) }
        state.error?.let {
            Text(
                text = stringResource(it.message()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = { onEvent(CoachDebugEvent.InputChanged(it)) },
                label = { Text(stringResource(R.string.coach_debug_message)) },
                modifier = Modifier.weight(1f),
            )
            if (state.answering) {
                Button(onClick = { onEvent(CoachDebugEvent.Stop) }) {
                    Text(stringResource(R.string.ai_lab_stop))
                }
            } else {
                Button(
                    onClick = { onEvent(CoachDebugEvent.Send) },
                    enabled = state.input.isNotBlank(),
                ) {
                    Text(stringResource(R.string.ai_lab_send))
                }
            }
        }
    }
}

@StringRes
private fun CoachError.message() = when (this) {
    CoachError.QUOTA_EXCEEDED -> R.string.coach_debug_error_quota
    CoachError.NETWORK -> R.string.coach_debug_error_network
    CoachError.BLOCKED -> R.string.coach_debug_error_blocked
    CoachError.TOO_MANY_TOOL_CALLS -> R.string.coach_debug_error_too_many_tools
    CoachError.UNKNOWN -> R.string.coach_debug_error_unknown
}

@Composable
private fun CoachTurnStats.text() = stringResource(
    R.string.coach_debug_stats,
    tools.size,
    totalMillis,
    usage.inputTokens,
    usage.outputTokens,
    usage.thinkingTokens,
)
