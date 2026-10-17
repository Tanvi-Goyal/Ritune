package com.ritune.feature.player.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritune.core.domain.models.player.RunProgress
import com.ritune.core.domain.usecases.player.ObservePlayerState
import com.ritune.core.domain.usecases.player.ObserveRunProgress
import com.ritune.core.domain.usecases.player.PauseRun
import com.ritune.core.domain.usecases.player.ResumeRun
import com.ritune.core.domain.usecases.player.SkipStep
import com.ritune.core.domain.usecases.player.StopRun
import com.ritune.core.models.player.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    observeRunProgress: ObserveRunProgress,
    observePlayerState: ObservePlayerState,
    private val pauseRun: PauseRun,
    private val resumeRun: ResumeRun,
    private val skipStep: SkipStep,
    private val stopRun: StopRun,
) : ViewModel() {

    private val confirmingStop = MutableStateFlow(false)

    val state: StateFlow<PlayerUiState> =
        combine(
            observeRunProgress(),
            confirmingStop
        ) { progress, confirming ->
            toUiState(progress, confirming)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PlayerUiState.Loading
        )

    private val _effects = Channel<PlayerUiEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerUiEffect> = _effects.receiveAsFlow()

    init {
        // One effect per transition: StateFlow already drops repeats of the same state.
        viewModelScope.launch {
            observePlayerState().collect { playerState ->
                when (playerState) {
                    PlayerState.Idle -> _effects.trySend(PlayerUiEffect.Close)
                    is PlayerState.Finished -> _effects.trySend(
                        PlayerUiEffect.RunFinished(
                            playerState.summary.runId.value
                        )
                    )

                    is PlayerState.Running, is PlayerState.Paused -> Unit
                }
            }
        }
    }

    fun onEvent(event: PlayerUiEvent) {
        when (event) {
            PlayerUiEvent.TogglePause -> viewModelScope.launch {
                if ((state.value as? PlayerUiState.Active)?.isPaused == true)
                    resumeRun() else pauseRun()
            }

            PlayerUiEvent.Skip -> viewModelScope.launch { skipStep() }
            PlayerUiEvent.RequestStop -> confirmingStop.value = true
            PlayerUiEvent.DismissStop -> confirmingStop.value = false
            PlayerUiEvent.ConfirmStop -> viewModelScope.launch {
                confirmingStop.value = false
                stopRun()
            }
        }
    }

    private fun toUiState(
        progress: RunProgress,
        confirming: Boolean
    ): PlayerUiState {
        val (session, paused) = when (val current = progress.state) {
            is PlayerState.Running -> current.session to false
            is PlayerState.Paused -> current.session to true
            else -> return PlayerUiState.Loading
        }

        val steps = session.routine.steps
        return PlayerUiState.Active(
            routineName = session.routine.name,
            stepIndex = session.stepIndex,
            stepCount = steps.size,
            stepTitle = session.currentStep.title,
            instruction = session.currentStep.instruction,
            nextStepTitle = steps.getOrNull(session.stepIndex + 1)?.title,
            remaining = progress.remaining,
            isPaused = paused,
            confirmingStop = confirming,
        )
    }
}
