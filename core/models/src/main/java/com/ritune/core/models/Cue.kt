package com.ritune.core.models

sealed interface Cue {
    data class Haptic(val pattern: HapticPattern) : Cue
    data class Sound(val sound: SoundCue) : Cue
    data class Speech(val text: String) : Cue
}

enum class HapticPattern { TICK, STEP_CHANGE, COMPLETE }
enum class SoundCue { SOFT_BELL, TICK, CHIME }

/** The cues every step gets: a buzz, a bell and its name read aloud. Settings decide which play. */
fun defaultStepCues(title: String): List<Cue> = listOf(
    Cue.Haptic(HapticPattern.STEP_CHANGE),
    Cue.Speech(title.trim()),
    Cue.Sound(SoundCue.SOFT_BELL),
)

