package com.ritune.feature.rhythm.ui.mapper

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter

/** Weekday names in the user's language: "M" for the bar label, "Monday" for TalkBack. */
internal class WeekdayFormatters(
    private val narrow: DateTimeFormatter,
    private val full: DateTimeFormatter,
) {
    fun narrow(day: LocalDate): String = narrow.format(day.toJavaLocalDate())
    fun full(day: LocalDate): String = full.format(day.toJavaLocalDate())
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun rememberWeekdayFormatters(): WeekdayFormatters {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        WeekdayFormatters(
            narrow = DateTimeFormatter.ofPattern("EEEEE", locale),
            full = DateTimeFormatter.ofPattern("EEEE", locale),
        )
    }
}

