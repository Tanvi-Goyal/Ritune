package com.ritune.feature.routine.ui.navigation.screens.routinedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.components.PrimaryButton
import com.ritune.core.designsystem.components.RituneAlertDialog
import com.ritune.core.designsystem.components.RituneCard
import com.ritune.core.designsystem.components.RituneDivider
import com.ritune.core.designsystem.components.RituneSnackbarHost
import com.ritune.core.designsystem.components.SecondaryButton
import com.ritune.core.designsystem.theme.Ritune
import com.ritune.feature.routine.ui.navigation.screens.formatDuration
import com.ritune.feature.routine.ui.navigation.screens.routineSummary

@Composable
fun RoutineDetailScreen(
    viewModel: RoutineDetailViewModel,
    onBack: () -> Unit,
    onEdit: (routineId: String) -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RoutineDetailEffect.OpenPlayer -> onOpenPlayer()
                RoutineDetailEffect.Deleted -> onBack()
                RoutineDetailEffect.RoutineUnavailable ->
                    snackbarHostState.showSnackbar(
                        resources.getString(
                            R.string.routines_unavailable
                        )
                    )
            }
        }
    }

    RoutineDetailContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onEdit = onEdit,
        modifier = modifier,
    )
}

@Composable
fun RoutineDetailContent(
    state: RoutineDetailUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (RoutineDetailEvent) -> Unit,
    onBack: () -> Unit,
    onEdit: (routineId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        when (state) {
            RoutineDetailUiState.Loading -> Unit
            RoutineDetailUiState.NotFound -> NotFound(onBack)
            is RoutineDetailUiState.Content -> DetailBody(state, onEvent, onBack, onEdit)
        }

        RituneSnackbarHost(
            snackbarHostState,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(Ritune.spacing.lg)
        )
    }
}

@Composable
private fun BackLink(onBack: () -> Unit) {
    TextButton(onClick = onBack) {
        Text(
            stringResource(R.string.detail_back),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NotFound(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .padding(Ritune.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Ritune.spacing.md)
    ) {
        BackLink(onBack)
        RituneCard {
            Column(
                Modifier.padding(Ritune.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Ritune.spacing.xs)
            ) {
                Text(
                    stringResource(R.string.detail_not_found_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface
                )
                Text(
                    stringResource(R.string.detail_not_found_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailBody(
    state: RoutineDetailUiState.Content,
    onEvent: (RoutineDetailEvent) -> Unit,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val summary = routineSummary(
        totalDuration = state.totalDuration,
        stepCount = state.steps.size,
        anchorText = state.anchorName?.let {
            stringResource(
                R.string.routines_logs_anchor,
                it
            )
        },
    )

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Ritune.spacing.xl)
                .padding(top = Ritune.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Ritune.spacing.lg),
        ) {
            BackLink(onBack)
            Column(verticalArrangement = Arrangement.spacedBy(Ritune.spacing.xs)) {
                Text(
                    state.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onBackground
                )
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            RituneCard {
                state.steps.forEachIndexed { index, step ->
                    if (index > 0) RituneDivider()
                    StepLine(position = index + 1, step = step)
                }
            }
            if (!state.builtIn) {
                TextButton(onClick = { onEvent(RoutineDetailEvent.RequestDelete) }) {
                    Text(
                        stringResource(R.string.detail_delete),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Ritune.spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.md),
        ) {
            PrimaryButton(
                text = stringResource(R.string.detail_start),
                onClick = { onEvent(RoutineDetailEvent.Start) },
                modifier = Modifier.weight(2f),
            )
            SecondaryButton(
                text = stringResource(R.string.detail_edit),
                onClick = { onEdit(state.id) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (state.confirmingDelete) {
        RituneAlertDialog(
            title = stringResource(R.string.detail_delete_title),
            text = stringResource(R.string.detail_delete_body, state.name),
            confirmLabel = stringResource(R.string.detail_delete_confirm),
            dismissLabel = stringResource(R.string.detail_cancel),
            onConfirm = { onEvent(RoutineDetailEvent.ConfirmDelete) },
            onDismiss = { onEvent(RoutineDetailEvent.DismissDelete) },
        )
    }
}

@Composable
private fun StepLine(position: Int, step: StepItem) {
    val colors = MaterialTheme.colorScheme
    val duration = formatDuration(step.duration)
    val description =
        stringResource(
            R.string.detail_step_description,
            position,
            step.title,
            duration
        )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = Ritune.spacing.lg, vertical = Ritune.spacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.md),
    ) {
        Text(
            "$position",
            style = MaterialTheme.typography.titleSmall,
            color = colors.primary,
            modifier = Modifier.width(20.dp)
        )
        Text(
            step.title,
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = duration,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
    }
}