package com.ritune.core.models

sealed interface Cue {
    data class Haptic(val pattern: HapticPattern) : Cue
    data class Sound(val sound: SoundCue) : Cue
    data class Speech(val text: String) : Cue
}

enum class HapticPattern { TICK, STEP_CHANGE, COMPLETE }
enum class SoundCue { SOFT_BELL, TICK, CHIME }

