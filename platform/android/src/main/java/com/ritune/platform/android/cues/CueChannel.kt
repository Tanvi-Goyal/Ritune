package com.ritune.platform.android.cues

import androidx.annotation.MainThread
import com.ritune.core.models.Cue

@MainThread
interface CueChannel {
    fun handles(cue: Cue): Boolean
    fun play(cue: Cue)
    fun prepare() {}
    fun release() {}
}
