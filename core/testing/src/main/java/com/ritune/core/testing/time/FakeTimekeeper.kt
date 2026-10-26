package com.ritune.core.testing.time

import com.ritune.core.models.Moment
import com.ritune.core.models.Timekeeper
import com.ritune.core.testing.data.testInstant
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Instant

class FakeTimekeeper(
    wall: Instant = testInstant,
    monoMillis: Long = 0L,
    var zone: TimeZone = TimeZone.UTC,
) : Timekeeper {

    private var current = Moment(monoMillis, wall)

    override fun now(): Moment = current

    override fun timeZone(): TimeZone = zone

    fun advanceBy(duration: Duration) {
        current = Moment(
            monoMillis = current.monoMillis + duration.inWholeMilliseconds,
            wall = current.wall + duration,
        )
    }

    fun setWall(wall: Instant) {
        current = current.copy(wall = wall)
    }
}
