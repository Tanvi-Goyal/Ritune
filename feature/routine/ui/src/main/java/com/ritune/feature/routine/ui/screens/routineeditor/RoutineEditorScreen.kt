package com.ritune.feature.routine.ui.screens.routineeditor

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.components.PrimaryButton
import com.ritune.core.designsystem.components.RituneAlertDialog
import com.ritune.core.designsystem.components.RituneBottomSheet
import com.ritune.core.designsystem.components.RituneCard
import com.ritune.core.designsystem.components.RituneDivider
import com.ritune.core.designsystem.components.RituneTextField
import com.ritune.core.designsystem.components.SecondaryButton
import com.ritune.core.designsystem.theme.Ritune
import com.ritune.core.domain.usecases.routine.RoutineIssue
import com.ritune.core.domain.usecases.routine.RoutineValidator
import com.ritune.feature.routine.ui.mapper.formatDuration

@Composable
fun RoutineEditorScreen(
    viewModel: RoutineEditorViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RoutineEditorEffect.Finished -> onFinished()
            }
        }
    }

    RoutineEditorContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
fun RoutineEditorContent(
    state: RoutineEditorUiState,
    onEvent: (RoutineEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        RoutineEditorUiState.Loading -> Box(modifier.fillMaxSize())
        is RoutineEditorUiState.Content -> EditorBody(state, onEvent, modifier)
    }
}

@Composable
private fun EditorBody(
    state: RoutineEditorUiState.Content,
    onEvent: (RoutineEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val nameIssue =
        state.issues.firstOrNull {
            it == RoutineIssue.NAME_BLANK || it == RoutineIssue.NAME_TOO_LONG
        }
    val stepsIssue =
        state.issues.firstOrNull {
            it != RoutineIssue.NAME_BLANK && it != RoutineIssue.NAME_TOO_LONG
        }
    val anchorName = state.anchors.firstOrNull { it.id == state.selectedAnchorId }?.name
        ?: stringResource(R.string.editor_anchor_none)

    // With unsaved changes, Back asks before discarding.
    BackHandler(enabled = state.isDirty) { onEvent(RoutineEditorEvent.CancelClicked) }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Ritune.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onEvent(RoutineEditorEvent.CancelClicked) }) {
                Text(
                    stringResource(R.string.editor_cancel),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(
                    if (state.isNew) R.string.editor_title_new
                    else R.string.editor_title_edit
                ),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onBackground,
            )
            TextButton(onClick = { onEvent(RoutineEditorEvent.SaveClicked) }) {
                Text(
                    stringResource(R.string.editor_save),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Ritune.spacing.xl)
                .padding(bottom = Ritune.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Ritune.spacing.lg),
        ) {
            RituneCard {
                Column(Modifier.padding(Ritune.spacing.lg)) {
                    RituneTextField(
                        value = state.name,
                        onValueChange = { onEvent(RoutineEditorEvent.NameChanged(it)) },
                        label = stringResource(R.string.editor_name_label),
                        isError = nameIssue != null,
                        supportingText = nameIssue?.message(),
                    )
                }
                RituneDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .clickable(
                            onClickLabel = stringResource(R.string.editor_anchor_change),
                            role = Role.Button
                        ) {
                            onEvent(RoutineEditorEvent.AnchorClicked)
                        }
                        .padding(horizontal = Ritune.spacing.lg, vertical = Ritune.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.md),
                ) {
                    Text(
                        stringResource(R.string.editor_anchor_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        anchorName,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Text(
                text = stringResource(R.string.editor_steps_heading),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Ritune.spacing.xs),
            )
            if (state.steps.isNotEmpty()) {
                RituneCard {
                    state.steps.forEachIndexed { index, row ->
                        if (index > 0) RituneDivider()
                        StepRowItem(index, row, onEvent)
                    }
                }
            }
            if (stepsIssue != null) {
                Text(
                    stepsIssue.message(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error
                )
            }
            SecondaryButton(
                text = stringResource(R.string.editor_add_step),
                onClick = { onEvent(RoutineEditorEvent.AddStepClicked) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.editor_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }

    when (val sheet = state.sheet) {
        null -> Unit
        EditorSheet.AnchorPicker -> RituneBottomSheet(onDismissRequest = {
            onEvent(
                RoutineEditorEvent.DismissSheet
            )
        }) {
            AnchorPickerContent(state.anchors, state.selectedAnchorId, onEvent)
        }

        is EditorSheet.StepEditor -> RituneBottomSheet(onDismissRequest = {
            onEvent(
                RoutineEditorEvent.DismissSheet
            )
        }) {
            StepEditorContent(sheet, onEvent)
        }
    }

    if (state.showDiscardDialog) {
        RituneAlertDialog(
            title = stringResource(R.string.editor_discard_title),
            text = stringResource(R.string.editor_discard_body),
            confirmLabel = stringResource(R.string.editor_discard_confirm),
            dismissLabel = stringResource(R.string.editor_keep_editing),
            onConfirm = { onEvent(RoutineEditorEvent.DiscardConfirmed) },
            onDismiss = { onEvent(RoutineEditorEvent.DiscardDismissed) },
        )
    }
}

@Composable
private fun StepRowItem(index: Int, row: StepRow, onEvent: (RoutineEditorEvent) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(
                start = Ritune.spacing.lg,
                end = Ritune.spacing.xs,
                top = Ritune.spacing.xs,
                bottom = Ritune.spacing.xs
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(
                    onClickLabel = stringResource(
                        R.string.editor_edit_step_label,
                        row.title
                    ), role = Role.Button
                ) {
                    onEvent(RoutineEditorEvent.EditStepClicked(index))
                }
                .semantics(mergeDescendants = true) { },
            verticalArrangement = Arrangement.Center,
        ) {
            Text(row.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Text(
                formatDuration(row.duration),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        MoveButton(
            "↑", stringResource(
                R.string.editor_move_up,
                row.title
            ), row.canMoveUp
        ) {
            onEvent(RoutineEditorEvent.MoveStep(index, -1))
        }
        MoveButton(
            "↓", stringResource(
                R.string.editor_move_down,
                row.title
            ),
            row.canMoveDown
        ) {
            onEvent(RoutineEditorEvent.MoveStep(index, 1))
        }
    }
}

@Composable
private fun MoveButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(symbol, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun AnchorPickerContent(
    anchors: List<AnchorOption>,
    selectedId: String?,
    onEvent: (RoutineEditorEvent) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Text(
        stringResource(R.string.editor_anchor_sheet_title),
        style = MaterialTheme.typography.titleMedium,
        color = colors.onSurface
    )
    Text(
        stringResource(R.string.editor_anchor_sheet_body),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant
    )
    ChoiceRow(
        stringResource(
            R.string.editor_anchor_none
        ),
        selected = selectedId == null
    ) {
        onEvent(RoutineEditorEvent.AnchorSelected(null))
    }
    anchors.forEach { anchor ->
        ChoiceRow(anchor.name, selected = anchor.id == selectedId) {
            onEvent(RoutineEditorEvent.AnchorSelected(anchor.id))
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.md),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun StepEditorContent(
    sheet: EditorSheet.StepEditor,
    onEvent: (RoutineEditorEvent) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val numbers = KeyboardOptions(keyboardType = KeyboardType.Number)
    val durationError = sheet.error == StepError.TOO_SHORT || sheet.error == StepError.TOO_LONG

    Text(
        text = stringResource(
            if (sheet.index == null)
                R.string.editor_step_sheet_new
            else R.string.editor_step_sheet_edit
        ),
        style = MaterialTheme.typography.titleMedium,
        color = colors.onSurface,
    )
    RituneTextField(
        value = sheet.title,
        onValueChange = { onEvent(RoutineEditorEvent.StepTitleChanged(it)) },
        label = stringResource(R.string.editor_step_title),
        isError = sheet.error == StepError.TITLE_BLANK,
        supportingText = if (sheet.error == StepError.TITLE_BLANK)
            stringResource(R.string.editor_step_error_title)
        else null,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.md)) {
        RituneTextField(
            value = sheet.minutes,
            onValueChange = { onEvent(RoutineEditorEvent.StepMinutesChanged(it)) },
            label = stringResource(R.string.editor_step_minutes),
            modifier = Modifier.weight(1f),
            isError = durationError,
            keyboardOptions = numbers,
        )
        RituneTextField(
            value = sheet.seconds,
            onValueChange = { onEvent(RoutineEditorEvent.StepSecondsChanged(it)) },
            label = stringResource(R.string.editor_step_seconds),
            modifier = Modifier.weight(1f),
            isError = durationError,
            keyboardOptions = numbers,
        )
    }
    if (durationError) {
        val message = if (sheet.error == StepError.TOO_SHORT) {
            stringResource(
                R.string.editor_step_error_short,
                RoutineValidator.MIN_STEP.inWholeSeconds.toInt()
            )
        } else {
            stringResource(
                R.string.editor_step_error_long,
                RoutineValidator.MAX_STEP.inWholeMinutes.toInt()
            )
        }
        Text(message, style = MaterialTheme.typography.bodySmall, color = colors.error)
    }
    RituneTextField(
        value = sheet.instruction,
        onValueChange = { onEvent(RoutineEditorEvent.StepInstructionChanged(it)) },
        label = stringResource(R.string.editor_step_instruction),
        singleLine = false,
    )
    PrimaryButton(
        text = stringResource(R.string.editor_step_done),
        onClick = { onEvent(RoutineEditorEvent.StepSheetDone) },
        modifier = Modifier.fillMaxWidth(),
    )
    if (sheet.index != null) {
        TextButton(
            onClick = { onEvent(RoutineEditorEvent.DeleteStep(sheet.index)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.editor_step_delete),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RoutineIssue.message(): String = when (this) {
    RoutineIssue.NAME_BLANK -> stringResource(R.string.editor_issue_name_blank)
    RoutineIssue.NAME_TOO_LONG -> stringResource(
        R.string.editor_issue_name_too_long,
        RoutineValidator.MAX_NAME_LENGTH
    )

    RoutineIssue.NO_STEPS -> stringResource(R.string.editor_issue_no_steps)
    RoutineIssue.TOO_MANY_STEPS -> stringResource(
        R.string.editor_issue_too_many_steps,
        RoutineValidator.MAX_STEPS
    )

    RoutineIssue.TOTAL_TOO_LONG -> stringResource(
        R.string.editor_issue_total_too_long,
        RoutineValidator.MAX_TOTAL.inWholeHours.toInt()
    )

    RoutineIssue.STEP_TITLE_BLANK, RoutineIssue.STEP_TOO_SHORT, RoutineIssue.STEP_TOO_LONG ->
        stringResource(R.string.editor_issue_steps_invalid)
}