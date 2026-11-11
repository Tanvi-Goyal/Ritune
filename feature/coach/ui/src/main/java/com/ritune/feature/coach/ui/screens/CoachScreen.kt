package com.ritune.feature.coach.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.components.PrimaryButton
import com.ritune.core.designsystem.components.RituneCard
import com.ritune.core.designsystem.components.RituneTextField
import com.ritune.core.designsystem.components.SecondaryButton
import com.ritune.core.designsystem.theme.Ritune
import com.ritune.core.designsystem.theme.RituneShapes
import com.ritune.core.domain.usecases.coach.tools.AnchorEventsTool
import com.ritune.core.domain.usecases.coach.tools.AnchorsTool
import com.ritune.core.domain.usecases.coach.tools.RoutinesTool
import com.ritune.core.domain.usecases.coach.tools.RunHistoryTool
import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
import com.ritune.core.models.coach.CoachError

@Composable
fun CoachScreen(
    viewModel: CoachViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CoachContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun CoachContent(
    state: CoachUiState,
    onEvent: (CoachUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = Ritune.spacing.xl)
            .padding(top = Ritune.spacing.sm, bottom = Ritune.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Ritune.spacing.sm),
    ) {
        Header(onBack)
        when (state) {
            CoachUiState.Loading -> Box(Modifier.weight(1f))
            CoachUiState.Consent -> Consent(onEvent, onBack)
            is CoachUiState.Chat -> Chat(state, onEvent)
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    TextButton(onClick = onBack) {
        Text(
            stringResource(R.string.coach_back),
            style = MaterialTheme.typography.labelMedium,
            color = colors.primary,
        )
    }
    Text(
        stringResource(R.string.coach_title),
        style = MaterialTheme.typography.headlineLarge,
        color = colors.onBackground,
    )
}

// The buttons come after the text, so turning the coach on means scrolling past what is shared.
@Composable
private fun ColumnScope.Consent(onEvent: (CoachUiEvent) -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Ritune.spacing.lg),
    ) {
        Text(
            stringResource(R.string.coach_consent_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.onBackground,
        )
        Text(
            stringResource(R.string.coach_consent_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
        )
        RituneCard {
            Column(
                Modifier.padding(Ritune.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Ritune.spacing.sm),
            ) {
                Text(
                    stringResource(R.string.coach_consent_shared_heading),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                listOf(
                    R.string.coach_consent_shared,
                    R.string.coach_consent_use,
                    R.string.coach_consent_off,
                ).forEach {
                    Text(
                        stringResource(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Ritune.spacing.sm)) {
            PrimaryButton(
                text = stringResource(R.string.coach_consent_turn_on),
                onClick = { onEvent(CoachUiEvent.TurnOn) },
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = stringResource(R.string.coach_consent_not_now),
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ColumnScope.Chat(state: CoachUiState.Chat, onEvent: (CoachUiEvent) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    LaunchedEffect(state.items.size) {
        if (state.items.isNotEmpty()) listState.animateScrollToItem(state.items.lastIndex)
    }

    if (state.items.isEmpty()) {
        Starters(onEvent, Modifier.weight(1f))
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Ritune.spacing.md),
        ) {
            items(state.items, key = { it.id }) { item ->
                when (item) {
                    is CoachMessage -> Message(item)
                    is ProposalCard -> ProposalCardView(item, onEvent)
                }
            }
        }
    }

    val tool = state.activeTool
    val last = state.items.lastOrNull()
    val textStreaming = last is CoachMessage && !last.fromUser
    when {
        tool != null -> Activity(toolLabel(tool))
        state.answering && !state.waitingForApproval && !textStreaming -> Activity(R.string.coach_thinking)
    }
    state.error?.let { ErrorRow(it, onRetry = { onEvent(CoachUiEvent.Retry) }) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.sm),
    ) {
        RituneTextField(
            value = state.input,
            onValueChange = { onEvent(CoachUiEvent.InputChanged(it)) },
            label = stringResource(R.string.coach_input_label),
            modifier = Modifier.weight(1f),
            singleLine = false,
        )
        if (state.answering) {
            SecondaryButton(
                text = stringResource(R.string.coach_stop),
                onClick = { onEvent(CoachUiEvent.Stop) },
                compact = true,
            )
        } else {
            SecondaryButton(
                text = stringResource(R.string.coach_send),
                onClick = { onEvent(CoachUiEvent.Send) },
                enabled = state.input.isNotBlank(),
                compact = true,
            )
        }
    }
    Text(
        stringResource(R.string.coach_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
    )
}

@Composable
private fun Starters(onEvent: (CoachUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Ritune.spacing.sm),
    ) {
        Text(
            stringResource(R.string.coach_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        starterQuestions.forEach { id ->
            val question = stringResource(id)
            SecondaryButton(
                text = question,
                onClick = { onEvent(CoachUiEvent.Ask(question)) },
                compact = true,
            )
        }
    }
}

@Composable
private fun Message(message: CoachMessage) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth()) {
        if (message.fromUser) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .widthIn(max = 300.dp)
                    .background(colors.primaryContainer, RituneShapes.Card)
                    .padding(horizontal = Ritune.spacing.md, vertical = Ritune.spacing.sm),
            )
        } else {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBackground,
            )
        }
    }
}

@Composable
private fun Activity(@StringRes label: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.sm),
    ) {
        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorRow(error: CoachError, onRetry: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(error.message()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.coach_retry)) }
    }
}

private val starterQuestions = listOf(
    R.string.coach_starter_week,
    R.string.coach_starter_wake,
    R.string.coach_starter_routines,
    R.string.coach_starter_next,
)

@StringRes
private fun toolLabel(tool: String) = when (tool) {
    WeekRhythmTool.DEFINITION.name -> R.string.coach_tool_week
    AnchorsTool.DEFINITION.name -> R.string.coach_tool_anchors
    AnchorEventsTool.DEFINITION.name -> R.string.coach_tool_logs
    RunHistoryTool.DEFINITION.name -> R.string.coach_tool_runs
    RoutinesTool.DEFINITION.name -> R.string.coach_tool_routines
    else -> R.string.coach_tool_other
}

@StringRes
private fun CoachError.message() = when (this) {
    CoachError.QUOTA_EXCEEDED -> R.string.coach_error_quota
    CoachError.NETWORK -> R.string.coach_error_network
    CoachError.BLOCKED -> R.string.coach_error_blocked
    CoachError.TOO_MANY_TOOL_CALLS -> R.string.coach_error_too_many_tools
    CoachError.UNKNOWN -> R.string.coach_error_unknown
}
