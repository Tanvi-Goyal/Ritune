package com.ritune.feature.settings.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritune.core.domain.usecases.DeleteAllData
import com.ritune.core.domain.usecases.player.ObservePlayerState
import com.ritune.core.domain.usecases.settings.ObserveSettings
import com.ritune.core.domain.usecases.settings.UpdateSettings
import com.ritune.core.models.AppSettings
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettings,
    private val updateSettings: UpdateSettings,
    observePlayerState: ObservePlayerState,
    private val deleteAllData: DeleteAllData,
) : ViewModel() {

    private data class Local(
        val themeSheetOpen: Boolean = false,
        val confirmingDelete: Boolean = false,
        val deleting: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    val state: StateFlow<SettingsUiState> =
        combine(
            observeSettings(),
            observePlayerState(),
            local
        ) { settings, player, local ->
            buildState(settings, player, local)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsUiState.Loading
        )

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.HapticsChanged -> change {
                it.copy(hapticsEnabled = event.enabled)
            }

            is SettingsEvent.SpeechChanged -> change {
                it.copy(speechEnabled = event.enabled)
            }

            is SettingsEvent.SoundChanged -> change {
                it.copy(soundEnabled = event.enabled)
            }

            is SettingsEvent.CountdownChanged -> change {
                it.copy(countdownEnabled = event.enabled)
            }

            SettingsEvent.ThemeClicked -> local.update {
                it.copy(themeSheetOpen = true)
            }

            is SettingsEvent.ThemeSelected -> {
                change { it.copy(theme = event.theme) }
                local.update { it.copy(themeSheetOpen = false) }
            }

            SettingsEvent.DismissTheme -> local.update {
                it.copy(themeSheetOpen = false)
            }

            SettingsEvent.RequestDelete -> if (!routineActive()) local.update {
                it.copy(
                    confirmingDelete = true
                )
            }

            SettingsEvent.DismissDelete -> local.update {
                it.copy(confirmingDelete = false)
            }

            SettingsEvent.ConfirmDelete -> delete()
        }
    }

    private fun change(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { updateSettings(transform) }
    }

    private fun delete() {
        if (local.value.deleting || routineActive()) return
        viewModelScope.launch {
            local.update { it.copy(deleting = true) }
            val effect = try {
                deleteAllData()
                SettingsEffect.DataDeleted
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SettingsEffect.DeleteFailed
            }
            local.update { it.copy(deleting = false, confirmingDelete = false) }
            _effects.trySend(effect)
        }
    }

    private fun routineActive(): Boolean =
        (state.value as? SettingsUiState.Content)?.routineActive == true

    private fun buildState(settings: AppSettings, player: PlayerState, local: Local) =
        SettingsUiState.Content(
            haptics = settings.hapticsEnabled,
            speech = settings.speechEnabled,
            sound = settings.soundEnabled,
            countdown = settings.countdownEnabled,
            theme = settings.theme,
            routineActive = player is PlayerState.Running || player is PlayerState.Paused,
            themeSheetOpen = local.themeSheetOpen,
            confirmingDelete = local.confirmingDelete,
            deleting = local.deleting,
        )
}