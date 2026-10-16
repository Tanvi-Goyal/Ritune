package com.ritune.feature.routine.ui.navigation.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.ritune.core.designsystem.R
import kotlin.time.Duration

/** "8 min", "45 sec", or "8 min 30 sec". */
@Composable
internal fun formatDuration(duration: Duration): String {
    val total = duration.inWholeSeconds.toInt()
    val minutes = total / 60
    val seconds = total % 60
    return when {
        minutes == 0 -> stringResource(
            R.string.routines_duration_sec,
            seconds
        )

        seconds == 0 -> stringResource(
            R.string.routines_duration_min,
            minutes
        )

        else -> stringResource(
            R.string.routines_duration_min_sec,
            minutes, seconds
        )
    }
}

/** "8 min · 4 steps · Starts Shutdown". The anchor part is skipped when there is no anchor. */
@Composable
internal fun routineSummary(
    totalDuration: Duration,
    stepCount: Int,
    anchorText: String?
): String {
    val parts = buildList {
        add(formatDuration(totalDuration))
        add(
            pluralStringResource(
                R.plurals.routines_steps,
                stepCount,
                stepCount
            )
        )
        if (anchorText != null) add(anchorText)
    }
    return parts.joinToString(" · ")
}