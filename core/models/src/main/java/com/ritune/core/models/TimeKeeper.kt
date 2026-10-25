package com.ritune.core.models

import kotlinx.datetime.TimeZone

interface Timekeeper {
    fun now(): Moment
    fun timeZone(): TimeZone
}
