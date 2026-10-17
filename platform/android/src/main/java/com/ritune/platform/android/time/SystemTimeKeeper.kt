package com.ritune.platform.android.time


import android.os.SystemClock
import com.ritune.core.models.IdGenerator
import com.ritune.core.models.Moment
import com.ritune.core.models.Timekeeper
import kotlinx.datetime.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTimekeeper @Inject constructor() : Timekeeper {
    override fun now() = Moment(
        monoMillis = SystemClock.elapsedRealtime(),
        wall = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
    )
}

@Singleton
class UuidIdGenerator @Inject constructor() : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}