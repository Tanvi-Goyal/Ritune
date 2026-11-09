package com.ritune.ailab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritune.R

@Composable
fun AiLabScreen(
    viewModel: AiLabViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AiLabContent(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

@Composable
fun AiLabContent(
    state: AiLabUiState,
    onEvent: (AiLabEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.ai_lab_title), style = MaterialTheme.typography.titleMedium)

        ChipRow(
            title = stringResource(R.string.ai_lab_model),
            options = LabModel.entries,
            selected = state.model,
            label = { it.id.removePrefix("gemini-") },
            onSelect = { onEvent(AiLabEvent.ModelSelected(it)) },
        )

        ChipRow(
            title = stringResource(R.string.ai_lab_thinking),
            options = LabThinking.entries,
            selected = state.thinking,
            label = { it.name.lowercase() },
            onSelect = { onEvent(AiLabEvent.ThinkingSelected(it)) },
        )

        OutlinedTextField(
            value = state.input,
            onValueChange = { onEvent(AiLabEvent.InputChanged(it)) },
            label = { Text(stringResource(R.string.ai_lab_message)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.rememberChat,
                onCheckedChange = { onEvent(AiLabEvent.RememberChatChanged(it)) },
            )
            Text(stringResource(R.string.ai_lab_remember_chat), Modifier.weight(1f))
            if (state.streaming) {
                Button(onClick = { onEvent(AiLabEvent.Stop) }) {
                    Text(stringResource(R.string.ai_lab_stop))
                }
            } else {
                Button(
                    onClick = { onEvent(AiLabEvent.Send) },
                    enabled = state.input.isNotBlank(),
                ) {
                    Text(stringResource(R.string.ai_lab_send))
                }
            }
        }

        state.stats?.let { Text(it.text(), style = MaterialTheme.typography.labelMedium) }

        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            text = state.reply,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun <T> ChipRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun LabStats.text() = stringResource(
    R.string.ai_lab_stats,
    firstChunkMillis?.toString() ?: "-",
    totalMillis,
    inputTokens?.toString() ?: "-",
    outputTokens?.toString() ?: "-",
    thinkingTokens?.toString() ?: "-",
)
