package com.ritune.platform.android


import android.os.SystemClock
import com.ritune.core.models.IdGenerator
import com.ritune.core.models.Moment
import com.ritune.core.models.Timekeeper
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTimekeeper @Inject constructor() : Timekeeper {
    override fun now() = Moment(
        monoMillis = SystemClock.elapsedRealtime(),
        wall = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
    )

    override fun timeZone() = TimeZone.currentSystemDefault()
}