package com.ritune.feature.player.ui.mapper

import android.os.Build
import android.text.format.DateFormat
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/** A moment as a time of day in the device's time zone,
 * honoring the system 12 or 24 hour setting. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun rememberTimeOfDayFormatter(): (Instant) -> String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    return remember(locale, is24Hour) {
        val pattern = DateFormat.getBestDateTimePattern(
            locale,
            if (is24Hour) "Hm" else "hm"
        )
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        val format: (Instant) -> String = { instant ->
            formatter.format(instant.toJavaInstant().atZone(ZoneId.systemDefault()))
        }
        format
    }
}
