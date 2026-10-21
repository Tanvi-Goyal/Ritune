package com.ritune.feature.settings.ui.screens

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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.components.ChoiceRow
import com.ritune.core.designsystem.components.RituneAlertDialog
import com.ritune.core.designsystem.components.RituneBottomSheet
import com.ritune.core.designsystem.components.RituneCard
import com.ritune.core.designsystem.components.RituneDivider
import com.ritune.core.designsystem.components.RituneSnackbarHost
import com.ritune.core.designsystem.components.SwitchRow
import com.ritune.core.designsystem.theme.Ritune
import com.ritune.core.models.ThemeSetting

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                SettingsEffect.DataDeleted -> R.string.settings_deleted
                SettingsEffect.DeleteFailed -> R.string.settings_delete_failed
            }
            snackbarHostState.showSnackbar(resources.getString(message))
        }
    }

    SettingsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (SettingsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        if (state is SettingsUiState.Content)
            Body(state, onEvent, onBack)

        RituneSnackbarHost(
            snackbarHostState,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(Ritune.spacing.lg)
        )
    }
}

@Composable
private fun Body(
    state: SettingsUiState.Content,
    onEvent: (SettingsEvent) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Ritune.spacing.xl)
            .padding(top = Ritune.spacing.sm, bottom = Ritune.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Ritune.spacing.sm),
    ) {
        TextButton(onClick = onBack) {
            Text(
                stringResource(R.string.settings_back),
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary
            )
        }
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.onBackground
        )

        SectionLabel(stringResource(R.string.settings_cues_heading))
        RituneCard {
            SwitchRow(
                label = stringResource(R.string.settings_haptics),
                supportingText = stringResource(R.string.settings_haptics_hint),
                checked = state.haptics,
                onCheckedChange = { onEvent(SettingsEvent.HapticsChanged(it)) },
            )
            RituneDivider()
            SwitchRow(
                label = stringResource(R.string.settings_speech),
                supportingText = stringResource(R.string.settings_speech_hint),
                checked = state.speech,
                onCheckedChange = { onEvent(SettingsEvent.SpeechChanged(it)) },
            )
            RituneDivider()
            SwitchRow(
                label = stringResource(R.string.settings_sound),
                supportingText = stringResource(R.string.settings_sound_hint),
                checked = state.sound,
                onCheckedChange = { onEvent(SettingsEvent.SoundChanged(it)) },
            )
            RituneDivider()
            SwitchRow(
                label = stringResource(R.string.settings_countdown),
                supportingText = stringResource(R.string.settings_countdown_hint),
                checked = state.countdown,
                onCheckedChange = { onEvent(SettingsEvent.CountdownChanged(it)) },
            )
        }
        Text(
            stringResource(R.string.settings_cues_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Ritune.spacing.xs),
        )

        SectionLabel(stringResource(R.string.settings_general_heading))
        RituneCard {
            ValueRow(
                label = stringResource(R.string.settings_theme),
                value = state.theme.label(),
                onClick = { onEvent(SettingsEvent.ThemeClicked) },
            )
            RituneDivider()
            ValueRow(
                label = stringResource(R.string.settings_delete),
                hint = stringResource(
                    if (state.routineActive)
                        R.string.settings_delete_blocked
                    else R.string.settings_delete_hint
                ),
                enabled = !state.routineActive,
                onClick = { onEvent(SettingsEvent.RequestDelete) },
            )
        }
    }

    if (state.themeSheetOpen) {
        RituneBottomSheet(onDismissRequest = { onEvent(SettingsEvent.DismissTheme) }) {
            Text(
                stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
            ThemeSetting.entries.forEach { option ->
                ChoiceRow(
                    label = option.label(),
                    selected = option == state.theme,
                    onClick = { onEvent(SettingsEvent.ThemeSelected(option)) },
                )
            }
        }
    }

    if (state.confirmingDelete) {
        RituneAlertDialog(
            title = stringResource(R.string.settings_delete_title),
            text = stringResource(R.string.settings_delete_body),
            confirmLabel = stringResource(R.string.settings_delete_confirm),
            dismissLabel = stringResource(R.string.settings_cancel),
            onConfirm = { onEvent(SettingsEvent.ConfirmDelete) },
            onDismiss = { onEvent(SettingsEvent.DismissDelete) },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Ritune.spacing.xs, top = Ritune.spacing.md),
    )
}

@Composable
private fun ValueRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Ritune.spacing.lg, vertical = Ritune.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) colors.onSurface else colors.onSurfaceVariant
            )
            if (hint != null) Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        if (value != null) Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeSetting.label(): String = stringResource(
    when (this) {
        ThemeSetting.SYSTEM -> R.string.settings_theme_system
        ThemeSetting.LIGHT -> R.string.settings_theme_light
        ThemeSetting.DARK -> R.string.settings_theme_dark
    },
)