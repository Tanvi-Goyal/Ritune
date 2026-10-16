package com.ritune.feature.routine.ui.navigation.screens.routineslist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.components.RituneCard
import com.ritune.core.designsystem.components.RituneDivider
import com.ritune.core.designsystem.components.RituneSnackbarHost
import com.ritune.core.designsystem.components.SecondaryButton
import com.ritune.core.designsystem.theme.Ritune
import com.ritune.feature.routine.ui.navigation.screens.routineSummary
import androidx.compose.ui.platform.LocalResources

@Composable
fun RoutinesListScreen(
    viewModel: RoutinesListViewModel,
    onOpenRoutine: (String) -> Unit,
    onNewRoutine: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RoutinesEffect.OpenPlayer -> onOpenPlayer()
                RoutinesEffect.RoutineUnavailable ->
                    snackbarHostState.showSnackbar(
                        resources.getString(
                            R.string.routines_unavailable
                        )
                    )
            }
        }
    }

    RoutinesListContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onOpenRoutine = onOpenRoutine,
        onNewRoutine = onNewRoutine,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@Composable
fun RoutinesListContent(
    state: RoutinesUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (RoutinesEvent) -> Unit,
    onOpenRoutine: (String) -> Unit,
    onNewRoutine: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        if (state is RoutinesUiState.Content) {
            ListBody(state, onEvent, onOpenRoutine, onNewRoutine, onOpenSettings)
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
private fun ListBody(
    state: RoutinesUiState.Content,
    onEvent: (RoutinesEvent) -> Unit,
    onOpenRoutine: (String) -> Unit,
    onNewRoutine: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = Ritune.spacing.xl,
                end = Ritune.spacing.xl,
                top = Ritune.spacing.lg,
                bottom = Ritune.spacing.xl
            ),
        verticalArrangement = Arrangement.spacedBy(Ritune.spacing.lg),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.routines_title),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onBackground
            )
            TextButton(onClick = onOpenSettings) {
                Text(
                    stringResource(R.string.routines_settings),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary
                )
            }
        }

        if (state.routines.isEmpty()) {
            RituneCard {
                Column(
                    Modifier.padding(Ritune.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Ritune.spacing.xs)
                ) {
                    Text(
                        stringResource(R.string.routines_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                    Text(
                        stringResource(R.string.routines_empty_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        } else {
            RituneCard {
                state.routines.forEachIndexed { index, item ->
                    if (index > 0) RituneDivider()
                    RoutineRow(
                        item,
                        onOpen = { onOpenRoutine(item.id) },
                        onStart = { onEvent(RoutinesEvent.Start(item.id)) })
                }
            }
        }

        SecondaryButton(
            text = stringResource(R.string.routines_new),
            onClick = onNewRoutine,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RoutineRow(item: RoutineItem, onOpen: () -> Unit, onStart: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val summary = routineSummary(
        totalDuration = item.totalDuration,
        stepCount = item.stepCount,
        anchorText = item.anchorName?.let {
            stringResource(R.string.routines_starts_anchor, it)
        },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .padding(horizontal = Ritune.spacing.lg, vertical = Ritune.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.md),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(
                    onClickLabel = stringResource(
                        R.string.routines_open_label,
                        item.name
                    ),
                    role = Role.Button,
                    onClick = onOpen
                )
                .semantics(mergeDescendants = true) { },
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface
            )
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        SecondaryButton(
            text = stringResource(R.string.routines_start),
            onClick = onStart,
            compact = true
        )
    }
}