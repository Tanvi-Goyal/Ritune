package com.ritune.platform.android.cues

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.annotation.RawRes
import com.ritune.core.models.Cue
import com.ritune.core.models.SoundCue
import com.ritune.platform.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Short clips through SoundPool: decoded once at [prepare], so playing has no latency at step change.
 * Loading is async and the first step starts right after [prepare], so a cue that arrives
 * before its clip is ready waits and plays as soon as the load completes.
 */
@Singleton
class SoundCueChannel @Inject constructor(
    @ApplicationContext private val context: Context,
) : CueChannel {

    private var pool: SoundPool? = null
    private var sampleIds: Map<SoundCue, Int> = emptyMap()
    private val loaded = mutableSetOf<Int>()
    private val waiting = mutableSetOf<Int>()

    override fun handles(cue: Cue) = cue is Cue.Sound

    override fun prepare() {
        if (pool != null) return
        val newPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        // Created on the main thread, so this listener also runs there.
        newPool.setOnLoadCompleteListener { loadedPool, sampleId, status ->
            if (status != LOAD_OK) {
                Log.w(TAG, "Failed to load sample $sampleId (status $status)")
                waiting -= sampleId
                return@setOnLoadCompleteListener
            }
            loaded += sampleId
            if (waiting.remove(sampleId)) loadedPool.playOnce(sampleId)
        }
        sampleIds = SoundCue.entries.associateWith { newPool.load(context, rawFor(it), PRIORITY) }
        pool = newPool
    }

    override fun play(cue: Cue) {
        if (cue !is Cue.Sound) return
        val pool = pool ?: return
        val sampleId = sampleIds[cue.sound] ?: return
        if (sampleId in loaded) pool.playOnce(sampleId) else waiting += sampleId
    }

    override fun release() {
        pool?.release()
        pool = null
        waiting.clear()
        sampleIds = emptyMap()
        loaded.clear()
    }

    private fun SoundPool.playOnce(sampleId: Int) {
        play(sampleId, VOLUME, VOLUME, PRIORITY, NO_LOOP, NORMAL_RATE)
    }

    @RawRes
    private fun rawFor(sound: SoundCue): Int = when (sound) {
        SoundCue.SOFT_BELL -> R.raw.soft_bell
        SoundCue.TICK -> R.raw.tick
        SoundCue.CHIME -> R.raw.chime
    }

    private companion object {
        const val TAG = "SoundCueChannel"
        const val MAX_STREAMS = 2 // a bell can overlap a countdown tick
        const val LOAD_OK = 0
        const val PRIORITY = 1
        const val VOLUME = 1f
        const val NO_LOOP = 0
        const val NORMAL_RATE = 1f
    }
}
