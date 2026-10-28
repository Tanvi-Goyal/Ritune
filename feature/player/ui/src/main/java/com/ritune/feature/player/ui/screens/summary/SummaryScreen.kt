package com.ritune.feature.player.ui.screens.summary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.components.PrimaryButton
import com.ritune.core.designsystem.components.RituneCard
import com.ritune.core.designsystem.components.formatTimer
import com.ritune.core.designsystem.theme.Ritune
import com.ritune.feature.player.ui.mapper.rememberTimeOfDayFormatter

@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SummaryUiEffect.Close -> onDone()
            }
        }
    }

    BackHandler {
        viewModel.onEvent(SummaryUiEvent.Done)
    }

    SummaryContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
fun SummaryContent(
    state: SummaryUiState,
    onEvent: (SummaryUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (state) {
                SummaryUiState.Loading -> Unit
                SummaryUiState.NotFound -> NotFoundBody()
                is SummaryUiState.Content -> ContentBody(state)
            }
        }

        if (state !is SummaryUiState.Loading) {
            PrimaryButton(
                text = stringResource(R.string.summary_done),
                onClick = { onEvent(SummaryUiEvent.Done) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Ritune.spacing.xl),
            )
        }
    }
}

@Composable
private fun ContentBody(state: SummaryUiState.Content) {
    val colors = MaterialTheme.colorScheme
    val formatTime = rememberTimeOfDayFormatter()
    val title = stringResource(
        when {
            !state.completed -> R.string.summary_title_stopped
            state.completedSteps == 0 -> R.string.summary_title_skipped
            else -> R.string.summary_title_done
        },
        state.routineName ?: stringResource(R.string.summary_routine_fallback),
    )
    val steps = when {
        !state.completed -> pluralStringResource(
            R.plurals.summary_steps_stopped,
            state.totalSteps,
            state.completedSteps,
            state.totalSteps
        )

        state.skippedSteps > 0 ->
            pluralStringResource(
                R.plurals.summary_steps_skipped,
                state.totalSteps,
                state.completedSteps,
                state.totalSteps,
                state.skippedSteps
            )

        else -> pluralStringResource(
            R.plurals.summary_steps,
            state.totalSteps,
            state.completedSteps, state.totalSteps
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < COMPACT_HEIGHT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Ritune.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutcomeBadge(
                completed = state.completed,
                diameter = if (compact) 64.dp else 96.dp,
                modifier = Modifier.padding(
                    top = if (compact) Ritune.spacing.xl else Ritune.spacing.xxl * 3
                ),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = if (compact) Ritune.spacing.lg else Ritune.spacing.xxl),
            )
            Text(
                text = steps,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Ritune.spacing.xs),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (compact) Ritune.spacing.xl else Ritune.spacing.xxl * 2),
                horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.md),
            ) {
                StatCard(
                    stringResource(R.string.summary_duration),
                    formatTimer(state.duration),
                    Modifier.weight(1f)
                )
                StatCard(
                    stringResource(R.string.summary_started),
                    formatTime(state.startedAt),
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NotFoundBody() {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .padding(Ritune.spacing.xl)
    ) {
        RituneCard {
            Column(
                Modifier.padding(Ritune.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Ritune.spacing.xs)
            ) {
                Text(
                    stringResource(R.string.summary_not_found_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface
                )
                Text(
                    stringResource(R.string.summary_not_found_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    RituneCard(modifier) {
        Column(Modifier.padding(Ritune.spacing.lg)) {
            Text(
                label, style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.headlineLarge, color = colors.onSurface)
        }
    }
}

@Composable
private fun OutcomeBadge(completed: Boolean, diameter: Dp, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val circle = if (completed) colors.primaryContainer else colors.surfaceVariant
    val mark = if (completed) colors.primary else colors.onSurfaceVariant
    Canvas(
        modifier
            .size(diameter)
            .clearAndSetSemantics { }) {
        drawCircle(color = circle)
        if (completed) {
            val check = Path().apply {
                moveTo(size.width * 0.31f, size.height * 0.52f)
                lineTo(size.width * 0.45f, size.height * 0.66f)
                lineTo(size.width * 0.70f, size.height * 0.37f)
            }
            drawPath(
                check,
                mark,
                style = Stroke(
                    width = 7.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        } else {
            val side = size.width * 0.26f
            drawRoundRect(
                color = mark,
                topLeft = Offset(
                    (size.width - side) / 2,
                    (size.height - side) / 2
                ),
                size = Size(side, side),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }
    }
}

private val COMPACT_HEIGHT = 480.dp
