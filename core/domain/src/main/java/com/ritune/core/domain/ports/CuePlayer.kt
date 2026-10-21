package com.ritune.core.domain.ports

import com.ritune.core.models.Cue

interface CuePlayer {
    fun prepare()                    // warm up TTS/SoundPool at run start
    fun play(cues: List<Cue>)        // fire-and-forget, must not block the effect collector
    fun countdown(secondsLeft: Int)  // 3, 2, 1
    fun release()
}