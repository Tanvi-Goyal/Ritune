package com.ritune.feature.rhythm.ui.navigation.screens

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.components.RituneCard
import com.ritune.core.designsystem.components.RituneDivider
import com.ritune.core.designsystem.components.WeekBars
import com.ritune.core.designsystem.theme.Ritune
import com.ritune.core.domain.usecases.rhythm.ConsistencyCalculator
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RhythmScreen(
    viewModel: RhythmViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RhythmContent(
        state = state,
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RhythmContent(
    state: RhythmUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        RhythmUiState.Loading -> Box(modifier.fillMaxSize())
        is RhythmUiState.Content -> RhythmBody(
            state,
            onOpenSettings,
            modifier
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun RhythmBody(
    state: RhythmUiState.Content,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
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
                    stringResource(R.string.rhythm_week_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.rhythm_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onBackground
                )
            }
            TextButton(onClick = onOpenSettings) {
                Text(
                    stringResource(R.string.rhythm_settings),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary
                )
            }
        }

        when (val summary = state.summary) {
            is RhythmSummary.Early -> MessageCard(
                title = stringResource(R.string.rhythm_early_title),
                body = stringResource(
                    R.string.rhythm_early_body,
                    summary.loggedDays,
                    summary.requiredDays
                ),
            )

            RhythmSummary.Building -> MessageCard(
                title = stringResource(R.string.rhythm_building_title),
                body = stringResource(R.string.rhythm_building_body),
            )

            is RhythmSummary.Scored -> {
                ConsistencyCard(
                    percent = summary.consistencyPercent,
                    days = state.days
                )
                StatsCard(
                    wakeDriftMinutes = summary.wakeDriftMinutes,
                    steadiestName = summary.steadiestName
                )
            }
        }
    }
}

@Composable
private fun MessageCard(title: String, body: String) {
    val colors = MaterialTheme.colorScheme
    RituneCard {
        Column(
            Modifier.padding(Ritune.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Ritune.spacing.xs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ConsistencyCard(percent: Int, days: List<DayBar>) {
    val colors = MaterialTheme.colorScheme
    val weekdays = rememberWeekdayFormatters()

    val labels = days.map { weekdays.narrow(it.day) }
    val descriptions = days.map { bar ->
        val name = weekdays.full(bar.day)
        val fraction = bar.fraction
        if (fraction == null) {
            stringResource(R.string.rhythm_day_no_data, name)
        } else {
            stringResource(
                R.string.rhythm_day_description,
                name, (fraction * 100).roundToInt()
            )
        }
    }

    RituneCard {
        Column(Modifier.padding(Ritune.spacing.lg)) {
            Text(
                stringResource(R.string.rhythm_consistency_label),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Text(
                stringResource(R.string.rhythm_percent, percent),
                style = MaterialTheme.typography.displayMedium,
                color = colors.primary
            )
            Text(
                stringResource(
                    R.string.rhythm_consistency_caption,
                    ConsistencyCalculator.TOLERANCE_MINUTES
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )

            WeekBars(
                values = days.map { it.fraction },
                labels = labels,
                descriptions = descriptions,
                modifier = Modifier.padding(top = Ritune.spacing.lg),
                highlightIndex = days.lastIndex, // the last bar is today
            )
        }
    }
}

@Composable
private fun StatsCard(wakeDriftMinutes: Int?, steadiestName: String?) {
    val none = stringResource(R.string.rhythm_value_none)
    RituneCard {
        StatRow(
            label = stringResource(R.string.rhythm_drift_label),
            value = wakeDriftMinutes?.let {
                pluralStringResource(
                    R.plurals.rhythm_minutes,
                    it,
                    it
                )
            }
                ?: none,
        )
        RituneDivider()
        StatRow(
            label = stringResource(R.string.rhythm_steadiest_label),
            value = steadiestName ?: none
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(
                horizontal = Ritune.spacing.lg,
                vertical = Ritune.spacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.md),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
    }
}