package com.ritune.feature.routine.ui.screens.routineeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ritune.core.domain.models.routine.SaveResult
import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.domain.usecases.routine.ObserveRoutine
import com.ritune.core.domain.usecases.routine.RoutineInput
import com.ritune.core.domain.usecases.routine.RoutineValidator
import com.ritune.core.domain.usecases.routine.SaveRoutine
import com.ritune.core.domain.usecases.routine.StepInput
import com.ritune.core.models.Anchor
import com.ritune.core.models.AnchorId
import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId
import com.ritune.core.models.StepId
import com.ritune.core.models.defaultStepCues
import com.ritune.feature.routine.ui.navigation.RoutineEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeRoutine: ObserveRoutine,
    observeAnchors: ObserveAnchors,
    private val saveRoutine: SaveRoutine,
) : ViewModel() {

    private val routineId: String? = savedStateHandle.toRoute<RoutineEditorRoute>().routineId

    private data class StepForm(
        val id: String?,
        val title: String,
        val seconds: Int,
        val instruction: String
    )

    private data class Form(
        val name: String = "",
        val anchorId: String? = null,
        val steps: List<StepForm> = emptyList()
    )

    private data class Edit(
        val form: Form,
        val original: Form,
        val attemptedSave: Boolean = false,
        val sheet: EditorSheet? = null,
        val showDiscard: Boolean = false,
    )

    private val edit = MutableStateFlow<Edit?>(null)

    val state: StateFlow<RoutineEditorUiState> =
        combine(edit, observeAnchors()) { current, anchors ->
            buildState(current, anchors)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            RoutineEditorUiState.Loading
        )

    private val _effects = Channel<RoutineEditorEffect>(Channel.BUFFERED)
    val effects: Flow<RoutineEditorEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (routineId == null) {
                edit.value = Edit(Form(), Form())
            } else {
                val routine = observeRoutine(RoutineId(routineId)).first()
                if (routine == null) {
                    _effects.trySend(RoutineEditorEffect.Finished)
                } else {
                    val form = routine.toForm()
                    edit.value = Edit(form, form)
                }
            }
        }
    }

    fun onEvent(event: RoutineEditorEvent) {
        when (event) {
            is RoutineEditorEvent.NameChanged -> updateForm {
                it.copy(name = event.value)
            }

            RoutineEditorEvent.AnchorClicked -> updateEdit {
                it.copy(sheet = EditorSheet.AnchorPicker)
            }

            is RoutineEditorEvent.AnchorSelected ->
                updateEdit {
                    it.copy(
                        form = it.form.copy(anchorId = event.anchorId),
                        sheet = null
                    )
                }

            RoutineEditorEvent.AddStepClicked -> updateEdit {
                it.copy(
                    sheet = EditorSheet.StepEditor(
                        null,
                        title = "",
                        minutes = "1",
                        seconds = "0",
                        instruction = "",
                        error = null
                    )
                )
            }

            is RoutineEditorEvent.EditStepClicked -> updateEdit { e ->
                val step = e.form.steps.getOrNull(event.index) ?: return@updateEdit e
                e.copy(
                    sheet = EditorSheet.StepEditor(
                        index = event.index,
                        title = step.title,
                        minutes = (step.seconds / 60).toString(),
                        seconds = (step.seconds % 60).toString(),
                        instruction = step.instruction,
                        error = null,
                    ),
                )
            }

            is RoutineEditorEvent.StepTitleChanged -> updateStepSheet {
                it.copy(
                    title = event.value,
                    error = null
                )
            }

            is RoutineEditorEvent.StepMinutesChanged ->
                updateStepSheet {
                    it.copy(
                        minutes = event.value.filter(Char::isDigit).take(3),
                        error = null
                    )
                }

            is RoutineEditorEvent.StepSecondsChanged ->
                updateStepSheet {
                    it.copy(
                        seconds = event.value.filter(Char::isDigit).take(2),
                        error = null
                    )
                }

            is RoutineEditorEvent.StepInstructionChanged ->
                updateStepSheet { it.copy(instruction = event.value) }

            RoutineEditorEvent.StepSheetDone -> commitStep()
            is RoutineEditorEvent.DeleteStep -> updateEdit {
                it.copy(
                    form = it.form.copy(
                        steps = it.form.steps.filterIndexed { i, _ -> i != event.index }),
                    sheet = null
                )
            }

            is RoutineEditorEvent.MoveStep -> moveStep(event.index, event.delta)
            RoutineEditorEvent.DismissSheet -> updateEdit { it.copy(sheet = null) }
            RoutineEditorEvent.SaveClicked -> save()
            RoutineEditorEvent.CancelClicked -> cancel()
            RoutineEditorEvent.DiscardConfirmed ->
                _effects.trySend(RoutineEditorEffect.Finished)

            RoutineEditorEvent.DiscardDismissed -> updateEdit { it.copy(showDiscard = false) }
        }
    }

    private fun save() {
        val current = edit.value ?: return
        viewModelScope.launch {
            when (saveRoutine(current.form.toInput())) {
                is SaveResult.Saved -> _effects.trySend(RoutineEditorEffect.Finished)
                is SaveResult.Invalid -> updateEdit { it.copy(attemptedSave = true) }
            }
        }
    }

    private fun cancel() {
        val current = edit.value ?: return
        if (current.form != current.original) {
            updateEdit { it.copy(showDiscard = true) }
        } else {
            _effects.trySend(RoutineEditorEffect.Finished)
        }
    }

    private fun commitStep() = updateEdit { e ->
        val sheet = e.sheet as? EditorSheet.StepEditor ?: return@updateEdit e
        val total = (sheet.minutes.toIntOrNull() ?: 0) * 60 + (sheet.seconds.toIntOrNull() ?: 0)
        val error = when {
            sheet.title.isBlank() -> StepError.TITLE_BLANK
            total < RoutineValidator.MIN_STEP.inWholeSeconds -> StepError.TOO_SHORT
            total > RoutineValidator.MAX_STEP.inWholeSeconds -> StepError.TOO_LONG
            else -> null
        }
        if (error != null) return@updateEdit e.copy(sheet = sheet.copy(error = error))

        val steps = e.form.steps.toMutableList()
        val index = sheet.index
        if (index == null || index !in steps.indices) {
            steps += StepForm(
                null,
                sheet.title.trim(),
                total,
                sheet.instruction.trim()
            )
        } else {
            steps[index] = steps[index].copy(
                title = sheet.title.trim(),
                seconds = total,
                instruction = sheet.instruction.trim()
            )
        }
        e.copy(form = e.form.copy(steps = steps), sheet = null)
    }

    private fun moveStep(index: Int, delta: Int) = updateForm { form ->
        val target = index + delta
        if (index !in form.steps.indices || target !in form.steps.indices) {
            form
        } else {
            form.copy(steps = form.steps.toMutableList().apply {
                add(target, removeAt(index))
            })
        }
    }

    private fun updateEdit(transform: (Edit) -> Edit) {
        edit.update { it?.let(transform) }
    }

    private fun updateForm(transform: (Form) -> Form) =
        updateEdit { it.copy(form = transform(it.form)) }

    private fun updateStepSheet(transform: (EditorSheet.StepEditor) -> EditorSheet.StepEditor) =
        updateEdit { e ->
            val sheet = e.sheet as? EditorSheet.StepEditor ?: return@updateEdit e
            e.copy(sheet = transform(sheet))
        }

    private fun buildState(current: Edit?, anchors: List<Anchor>): RoutineEditorUiState {
        if (current == null) return RoutineEditorUiState.Loading
        val form = current.form
        val last = form.steps.lastIndex
        return RoutineEditorUiState.Content(
            isNew = routineId == null,
            name = form.name,
            anchors = anchors.map { AnchorOption(it.id.value, it.name) },
            selectedAnchorId = form.anchorId,
            steps = form.steps.mapIndexed { i, s ->
                StepRow(
                    s.title,
                    s.seconds.seconds,
                    canMoveUp = i > 0,
                    canMoveDown = i < last
                )
            },
            issues = if (current.attemptedSave)
                RoutineValidator.validate(form.toInput())
            else emptySet(),
            sheet = current.sheet,
            showDiscardDialog = current.showDiscard,
            isDirty = form != current.original,
        )
    }

    private fun Form.toInput() = RoutineInput(
        id = routineId?.let(::RoutineId),
        name = name,
        anchorId = anchorId?.let(::AnchorId),
        steps = steps.map { step ->
            StepInput(
                id = step.id?.let(::StepId),
                title = step.title,
                duration = step.seconds.seconds,
                instruction = step.instruction.ifBlank { null },
                cues = defaultStepCues(step.title),
            )
        },
    )

    private fun Routine.toForm() = Form(
        name = name,
        anchorId = anchorId?.value,
        steps = steps.map {
            StepForm(
                it.id.value,
                it.title,
                it.duration.inWholeSeconds.toInt(),
                it.instruction.orEmpty()
            )
        },
    )
}