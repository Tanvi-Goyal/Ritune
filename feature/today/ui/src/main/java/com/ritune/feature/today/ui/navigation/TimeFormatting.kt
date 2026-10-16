package com.ritune.feature.today.ui.navigation

import android.os.Build
import android.text.format.DateFormat
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import java.time.format.DateTimeFormatter

/** Time of day in the user's locale, honouring the system 12 or 24 hour setting. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun rememberTimeFormatter(): (LocalTime) -> String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    return remember(locale, is24Hour) {
        val pattern = DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm")
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        val format: (LocalTime) -> String = { time ->
            formatter.format(time.toJavaLocalTime())
        }
        format
    }
}

/** "Friday, 9 October" in the user's locale and word order. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun rememberDateFormatter(): (LocalDate) -> String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        val formatter = DateTimeFormatter.ofPattern(
            DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"),
            locale
        )
        val format: (LocalDate) -> String = { date ->
            formatter.format(date.toJavaLocalDate())
        }
        format
    }
}

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

