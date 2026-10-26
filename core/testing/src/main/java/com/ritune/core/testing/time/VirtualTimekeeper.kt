package com.ritune.core.testing.time

import com.ritune.core.models.Moment
import com.ritune.core.models.Timekeeper
import com.ritune.core.testing.data.testInstant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class VirtualTimekeeper(
    private val scheduler: TestCoroutineScheduler,
    private val startWall: Instant = testInstant,
    var zone: TimeZone = TimeZone.UTC,
) : Timekeeper {

    override fun now(): Moment {
        val elapsed = scheduler.currentTime
        return Moment(monoMillis = elapsed, wall = startWall + elapsed.milliseconds)
    }

    override fun timeZone(): TimeZone = zone
}
