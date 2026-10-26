package com.ritune.core.testing.ports

import com.ritune.core.domain.ports.CuePlayer
import com.ritune.core.models.Cue

class FakeCuePlayer : CuePlayer {

    var prepared = false
        private set
    var released = false
        private set

    val played = mutableListOf<List<Cue>>()
    val countdowns = mutableListOf<Int>()

    override fun prepare() {
        prepared = true
    }

    override fun play(cues: List<Cue>) {
        played += cues
    }

    override fun countdown(secondsLeft: Int) {
        countdowns += secondsLeft
    }

    override fun release() {
        released = true
    }
}
