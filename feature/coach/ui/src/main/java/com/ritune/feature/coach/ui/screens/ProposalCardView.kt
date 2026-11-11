package com.ritune.feature.coach.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ritune.core.designsystem.R
import com.ritune.core.designsystem.components.PrimaryButton
import com.ritune.core.designsystem.components.RituneCard
import com.ritune.core.designsystem.components.SecondaryButton
import com.ritune.core.designsystem.theme.Ritune
import com.ritune.core.models.coach.Proposal
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration

@Composable
internal fun ProposalCardView(card: ProposalCard, onEvent: (CoachUiEvent) -> Unit) {
    val colors = MaterialTheme.colorScheme
    RituneCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(Ritune.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Ritune.spacing.sm),
        ) {
            Text(
                stringResource(card.proposal.title()),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
            ProposalDetails(card.proposal)
            Footer(card, onEvent)
        }
    }
}

@Composable
private fun ProposalDetails(proposal: Proposal) {
    val formatTime = rememberTimeFormatter()
    val body: @Composable (String) -> Unit = {
        Text(
            it, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    when (proposal) {
        is Proposal.AnchorTarget -> body(
            stringResource(
                R.string.coach_proposal_target_change,
                proposal.anchorName,
                proposal.from?.let(formatTime) ?: stringResource(R.string.coach_proposal_no_target),
                formatTime(proposal.to),
            ),
        )

        is Proposal.NewRoutine -> {
            Text(
                proposal.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            proposal.anchorName?.let {
                body(
                    stringResource(
                        R.string.coach_proposal_routine_anchor,
                        it
                    )
                )
            }
            proposal.steps.forEach { step ->
                body(
                    stringResource(
                        R.string.coach_proposal_routine_step,
                        step.title,
                        formatDuration(step.duration)
                    )
                )
            }
            body(
                stringResource(
                    R.string.coach_proposal_routine_total,
                    formatDuration(proposal.totalDuration)
                )
            )
        }

        is Proposal.LogCorrection -> body(
            stringResource(
                R.string.coach_proposal_log_change,
                proposal.anchorName,
                rememberDateFormatter()(proposal.date),
                formatTime(proposal.from),
                formatTime(proposal.to),
            ),
        )
    }
}

@Composable
private fun Footer(card: ProposalCard, onEvent: (CoachUiEvent) -> Unit) {
    when (card.status) {
        ProposalStatus.Pending ->
            Row(horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.sm)) {
                PrimaryButton(
                    text = stringResource(R.string.coach_proposal_apply),
                    onClick = { onEvent(CoachUiEvent.Apply) },
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = stringResource(R.string.coach_proposal_decline),
                    onClick = { onEvent(CoachUiEvent.Decline) },
                    modifier = Modifier.weight(1f),
                )
            }

        is ProposalStatus.Applied ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusText(R.string.coach_proposal_applied, Modifier.weight(1f))
                TextButton(onClick = { onEvent(CoachUiEvent.Undo(card.id)) }) {
                    Text(stringResource(R.string.coach_proposal_undo))
                }
            }

        ProposalStatus.Declined -> StatusText(R.string.coach_proposal_declined)
        ProposalStatus.NotApplied -> StatusText(R.string.coach_proposal_not_applied)
        ProposalStatus.Undone -> StatusText(R.string.coach_proposal_undone)
    }
}

@Composable
private fun StatusText(text: Int, modifier: Modifier = Modifier) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

private fun Proposal.title() = when (this) {
    is Proposal.AnchorTarget -> R.string.coach_proposal_target_title
    is Proposal.NewRoutine -> R.string.coach_proposal_routine_title
    is Proposal.LogCorrection -> R.string.coach_proposal_log_title
}

@Composable
private fun rememberTimeFormatter(): (LocalTime) -> String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    return remember(locale, is24Hour) {
        val formatter = DateTimeFormatter.ofPattern(
            DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"),
            locale,
        )
        val format: (LocalTime) -> String = { formatter.format(it.toJavaLocalTime()) }
        format
    }
}

@Composable
private fun rememberDateFormatter(): (LocalDate) -> String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        val formatter = DateTimeFormatter.ofPattern(
            DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"),
            locale
        )
        val format: (LocalDate) -> String = { formatter.format(it.toJavaLocalDate()) }
        format
    }
}

@Composable
private fun formatDuration(duration: Duration): String {
    val minutes = duration.inWholeMinutes.toInt()
    val seconds = (duration.inWholeSeconds % 60).toInt()
    return when {
        minutes == 0 -> stringResource(R.string.routines_duration_sec, seconds)
        seconds == 0 -> stringResource(R.string.routines_duration_min, minutes)
        else -> stringResource(R.string.routines_duration_min_sec, minutes, seconds)
    }
}
