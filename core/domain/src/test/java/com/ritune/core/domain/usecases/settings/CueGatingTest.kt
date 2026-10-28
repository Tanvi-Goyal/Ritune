package com.ritune.core.domain.usecases.settings

import com.ritune.core.models.AppSettings
import com.ritune.core.models.Cue
import com.ritune.core.models.HapticPattern
import com.ritune.core.models.SoundCue
import org.junit.Test
import kotlin.test.assertEquals

class CueGatingTest {

    private val haptic = Cue.Haptic(HapticPattern.TICK)
    private val sound = Cue.Sound(SoundCue.CHIME)
    private val speech = Cue.Speech("Stretch")
    private val cues = listOf(haptic, sound, speech)

    private val allOff = AppSettings(hapticsEnabled = false, speechEnabled = false, soundEnabled = false)

    private fun AppSettings.allowed() = cues.filter { allows(it) }

    @Test
    fun allows_everyChannelOff_blocksAll() {
        assertEquals(emptyList(), allOff.allowed())
    }

    @Test
    fun allows_eachSetting_unblocksOnlyItsCue() {
        assertEquals(listOf(haptic), allOff.copy(hapticsEnabled = true).allowed())
        assertEquals(listOf(sound), allOff.copy(soundEnabled = true).allowed())
        assertEquals(listOf(speech), allOff.copy(speechEnabled = true).allowed())
    }
}
