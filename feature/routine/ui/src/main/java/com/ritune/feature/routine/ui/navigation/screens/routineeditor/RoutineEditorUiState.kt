package com.ritune.feature.routine.ui.navigation.screens.routineeditor

import com.ritune.core.domain.usecases.routine.RoutineIssue
import kotlin.time.Duration

sealed interface RoutineEditorUiState {
    data object Loading : RoutineEditorUiState

    data class Content(
        val isNew: Boolean,
        val name: String,
        val anchors: List<AnchorOption>,
        val selectedAnchorId: String?,
        val steps: List<StepRow>,
        val issues: Set<RoutineIssue>,
        val sheet: EditorSheet?,
        val showDiscardDialog: Boolean,
        val isDirty: Boolean,
    ) : RoutineEditorUiState
}

data class AnchorOption(
    val id: String,
    val name: String
)

data class StepRow(
    val title: String,
    val duration: Duration,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean
)

sealed interface EditorSheet {
    data object AnchorPicker : EditorSheet

    data class StepEditor(
        val index: Int?,
        val title: String,
        val minutes: String,
        val seconds: String,
        val instruction: String,
        val error: StepError?,
    ) : EditorSheet
}

enum class StepError { TITLE_BLANK, TOO_SHORT, TOO_LONG }

sealed interface RoutineEditorEvent {
    data class NameChanged(val value: String) : RoutineEditorEvent
    data object AnchorClicked : RoutineEditorEvent

    data class AnchorSelected(val anchorId: String?) : RoutineEditorEvent
    data object AddStepClicked : RoutineEditorEvent
    data class EditStepClicked(val index: Int) : RoutineEditorEvent
    data class StepTitleChanged(val value: String) : RoutineEditorEvent
    data class StepMinutesChanged(val value: String) : RoutineEditorEvent
    data class StepSecondsChanged(val value: String) : RoutineEditorEvent
    data class StepInstructionChanged(val value: String) : RoutineEditorEvent
    data object StepSheetDone : RoutineEditorEvent
    data class DeleteStep(val index: Int) : RoutineEditorEvent
    data class MoveStep(val index: Int, val delta: Int) : RoutineEditorEvent
    data object DismissSheet : RoutineEditorEvent
    data object SaveClicked : RoutineEditorEvent
    data object CancelClicked : RoutineEditorEvent
    data object DiscardConfirmed : RoutineEditorEvent
    data object DiscardDismissed : RoutineEditorEvent
}

sealed interface RoutineEditorEffect {
    data object Finished : RoutineEditorEffect
}