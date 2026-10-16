package com.ritune.feature.player.ui.navigation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.components.PrimaryButton
import com.ritune.core.designsystem.components.RituneAlertDialog
import com.ritune.core.designsystem.components.SecondaryButton
import com.ritune.core.designsystem.components.StepProgress
import com.ritune.core.designsystem.components.TimerText
import com.ritune.core.designsystem.theme.Ritune

@Composable
fun PlayerScreen(
//    viewModel: PlayerViewModel,
    onFinished: (runId: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
//    val state by viewModel.state.collectAsStateWithLifecycle()
//
//    LaunchedEffect(viewModel) {
//        viewModel.effects.collect { effect ->
//            when (effect) {
//                PlayerUiEffect.Close -> onClose()
//                is PlayerUiEffect.RunFinished -> onFinished(effect.runId)
//            }
//        }
//    }
//
//    // The user opened this screen on purpose to watch the timer.
//    // The routine itself keeps running with the screen off, so this only stops the display
//    // sleeping while the player is visible.
//    val view = LocalView.current
//    DisposableEffect(view) {
//        view.keepScreenOn = true
//        onDispose { view.keepScreenOn = false }
//    }
//
//    PlayerContent(
//        state = state,
//        onEvent = viewModel::onEvent,
//        modifier = modifier
//    )
}

@Composable
fun PlayerContent(
    state: PlayerUiState,
    onEvent: (PlayerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        PlayerUiState.Loading -> Box(modifier.fillMaxSize())
        is PlayerUiState.Active -> ActiveBody(state, onEvent, modifier)
    }
}

@Composable
private fun ActiveBody(
    state: PlayerUiState.Active,
    onEvent: (PlayerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val stepCounter =
        stringResource(
            R.string.player_step_counter,
            state.stepIndex + 1, state.stepCount
        )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Ritune.spacing.xl)
            .padding(top = Ritune.spacing.lg),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryButton(
                text = stringResource(R.string.player_stop),
                onClick = { onEvent(PlayerUiEvent.RequestStop) },
                compact = true,
            )
            Text(
                stepCounter,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        Text(
            text = state.routineName,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onBackground,
            modifier = Modifier.padding(top = Ritune.spacing.xl),
        )

        StepProgress(
            total = state.stepCount,
            currentIndex = state.stepIndex,
            description = stepCounter,
            modifier = Modifier.padding(top = Ritune.spacing.md),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(
                    if (state.isPaused)
                        R.string.player_paused
                    else R.string.player_now
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = state.stepTitle,
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Ritune.spacing.xs),
            )
            if (state.instruction != null) {
                Text(
                    text = state.instruction,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Ritune.spacing.sm),
                )
            }
            TimerText(
                remaining = state.remaining,
                color = if (state.isPaused) colors.onSurfaceVariant else colors.primary,
                modifier = Modifier.padding(vertical = Ritune.spacing.xl),
            )
            Text(
                text = state.nextStepTitle?.let {
                    stringResource(
                        R.string.player_next,
                        it
                    )
                } ?: stringResource(R.string.player_last_step),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Ritune.spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.md),
        ) {
            PrimaryButton(
                text = stringResource(
                    if (state.isPaused)
                        R.string.player_resume else R.string.player_pause
                ),
                onClick = { onEvent(PlayerUiEvent.TogglePause) },
                modifier = Modifier.weight(2f),
            )

            SecondaryButton(
                text = stringResource(
                    if (state.isLastStep) R.string.player_finish
                    else R.string.player_skip
                ),
                onClick = { onEvent(PlayerUiEvent.Skip) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (state.confirmingStop) {
        RituneAlertDialog(
            title = stringResource(R.string.player_stop_title),
            text = stringResource(R.string.player_stop_body),
            confirmLabel = stringResource(R.string.player_stop_confirm),
            dismissLabel = stringResource(R.string.player_keep_going),
            onConfirm = { onEvent(PlayerUiEvent.ConfirmStop) },
            onDismiss = { onEvent(PlayerUiEvent.DismissStop) },
        )
    }
}
