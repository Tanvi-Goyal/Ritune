package com.ritune.feature.today.ui.screens

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.components.AnchorRow
import com.ritune.core.designsystem.components.ConsistencyRing
import com.ritune.core.designsystem.components.RituneCard
import com.ritune.core.designsystem.components.RituneDivider
import com.ritune.core.designsystem.components.RituneSnackbarHost
import com.ritune.core.designsystem.components.UNDO_TIMEOUT_MS
import com.ritune.core.designsystem.components.showUndo
import com.ritune.core.designsystem.theme.Ritune
import com.ritune.core.designsystem.theme.RituneShapes
import com.ritune.feature.today.ui.mapper.rememberDateFormatter
import com.ritune.feature.today.ui.mapper.rememberTimeFormatter
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    onOpenSettings: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val formatTime = rememberTimeFormatter()
    val resources = LocalResources.current
    val accessibilityManager = LocalAccessibilityManager.current

    ReportDrawnWhen { state !is TodayUiState.Loading }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            this@LaunchedEffect.launch {
                when (effect) {
                    is TodayEffect.Logged -> {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val message = resources.getString(
                            R.string.today_snackbar_logged,
                            effect.anchorName,
                            formatTime(effect.loggedAt)
                        )
                        val undone = snackbarHostState.showUndo(
                            message,
                            resources.getString(R.string.today_snackbar_undo),
                            timeoutMs = accessibilityManager?.calculateRecommendedTimeoutMillis(
                                originalTimeoutMillis = UNDO_TIMEOUT_MS,
                                containsText = true,
                                containsControls = true,
                            ) ?: UNDO_TIMEOUT_MS,
                        )

                        if (undone) viewModel.onEvent(TodayEvent.UndoLog(effect.eventId))
                    }

                    TodayEffect.OpenPlayer -> onOpenPlayer()
                    TodayEffect.RoutineUnavailable ->
                        snackbarHostState.showSnackbar(
                            resources.getString(
                                R.string.today_routine_unavailable
                            )
                        )
                }
            }
        }
    }

    TodayContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@Composable
fun TodayContent(
    state: TodayUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (TodayEvent) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        when (state) {
            TodayUiState.Loading -> Unit
            is TodayUiState.Content -> TodayUi(state, onEvent, onOpenSettings)
        }

        RituneSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Ritune.spacing.lg),
        )
    }
}

@Composable
private fun TodayUi(
    state: TodayUiState.Content,
    onEvent: (TodayEvent) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val formatTime = rememberTimeFormatter()
    val formatDate = rememberDateFormatter()

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
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    formatDate(state.day),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.today_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onBackground
                )
            }

            TextButton(onClick = onOpenSettings) {
                Text(
                    stringResource(R.string.today_settings),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary
                )
            }
        }

        SummaryCard(logged = state.loggedCount, total = state.totalCount)

        state.runningRoutineName?.let { name ->
            RunningBanner(name = name, onOpen = { onEvent(TodayEvent.OpenRunningRoutine) })
        }

        RituneCard {
            state.anchors.forEachIndexed { index, item ->
                if (index > 0) RituneDivider()
                AnchorItemRow(
                    item = item,
                    formatTime = formatTime,
                    onEvent = onEvent
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(logged: Int, total: Int) {
    val colors = MaterialTheme.colorScheme
    val (headline, subtitle) = when {
        total == 0 || logged == 0 ->
            stringResource(R.string.today_headline_empty) to
                    stringResource(R.string.today_subtitle_empty)

        logged >= total ->
            stringResource(R.string.today_headline_done) to
                    stringResource(R.string.today_subtitle_done)

        else ->
            stringResource(R.string.today_headline_partial) to
                    stringResource(
                        R.string.today_subtitle_partial,
                        logged,
                        total
                    )
    }

    RituneCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Ritune.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.lg),
        ) {
            ConsistencyRing(
                done = logged,
                total = total,
                description = pluralStringResource(
                    R.plurals.today_ring_description,
                    total,
                    logged, total
                )
            )

            Column {
                Text(
                    headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface
                )

                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnchorItemRow(
    item: AnchorItem,
    formatTime: (LocalTime) -> String,
    onEvent: (TodayEvent) -> Unit,
) {
    val logged = item.loggedAt
    val target = item.targetTime?.let(formatTime)
    val routine = item.routine

    AnchorRow(
        name = item.name,
        timeText = when {
            logged != null -> formatTime(logged)
            target != null -> stringResource(
                R.string.today_target_time,
                target
            )

            else -> ""
        },
        logged = logged != null,
        statusDescription = when {
            logged != null -> stringResource(
                R.string.today_status_logged,
                formatTime(logged)
            )

            target != null -> stringResource(
                R.string.today_status_pending_with_target,
                target
            )

            else -> stringResource(R.string.today_status_pending)
        },
        // Anchors with a routine get "Start routine"; the name side still logs without running it.
        actionLabel = if (routine != null) stringResource(
            R.string.today_action_start_routine
        ) else
            stringResource(R.string.today_action_log),
        onAction = {
            if (routine != null) onEvent(TodayEvent.StartRoutine(routine.id))
            else onEvent(TodayEvent.LogAnchor(item.id))
        },
        onClick = { onEvent(TodayEvent.LogAnchor(item.id)) },
        onClickLabel = stringResource(
            R.string.today_log_anchor_label,
            item.name
        ),
    )
}

@Composable
private fun RunningBanner(name: String, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val openLabel = stringResource(R.string.today_running_open)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RituneShapes.Card)
            .background(colors.primaryContainer)
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen)
            .heightIn(min = 56.dp)
            .padding(horizontal = Ritune.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(R.string.today_running_banner, name),
            style = MaterialTheme.typography.titleSmall,
            color = colors.onPrimaryContainer
        )
        Text(
            openLabel,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onPrimaryContainer
        )
    }
}