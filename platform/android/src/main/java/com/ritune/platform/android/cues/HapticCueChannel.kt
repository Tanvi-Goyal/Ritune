package com.ritune.platform.android.cues

import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission
import com.ritune.core.models.Cue
import com.ritune.core.models.HapticPattern
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticCueChannel @Inject constructor(
    @ApplicationContext context: Context,
) : CueChannel {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }

    override fun handles(cue: Cue) = cue is Cue.Haptic

    @RequiresPermission(Manifest.permission.VIBRATE)
    override fun play(cue: Cue) {
        if (cue !is Cue.Haptic) return
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return

        // Alarm usage: the user started a timer,
        // and some OEMs drop default-usage vibrations with the screen off.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(
                effectFor(cue.pattern),
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effectFor(cue.pattern),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
    }

    private fun effectFor(pattern: HapticPattern): VibrationEffect = when {
        pattern == HapticPattern.TICK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)

        else -> VibrationEffect.createWaveform(timingsFor(pattern), NO_REPEAT)
    }

    private fun timingsFor(pattern: HapticPattern): LongArray = when (pattern) {
        HapticPattern.TICK -> longArrayOf(0, 25)
        HapticPattern.STEP_CHANGE -> longArrayOf(0, 70, 70, 70)
        HapticPattern.COMPLETE -> longArrayOf(0, 120, 80, 120, 80, 260)
    }

    private companion object {
        const val NO_REPEAT = -1
    }
}
