package com.ritune.platform.android.cues

import android.util.Log
import com.ritune.core.domain.ports.CuePlayer
import com.ritune.core.domain.usecases.settings.ObserveSettings
import com.ritune.core.domain.usecases.settings.allows
import com.ritune.core.models.AppSettings
import com.ritune.core.models.Cue
import com.ritune.core.models.HapticPattern
import com.ritune.core.models.SoundCue
import com.ritune.platform.android.di.Dispatcher
import com.ritune.platform.android.di.PlayerEngineScope
import com.ritune.platform.android.di.RituneDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Routes cues to the channel that renders them, after checking settings.
 *
 * - Settings are cached in a StateFlow, so each cue reads the latest toggles with no I/O. A toggle
 *   changed mid-run applies from the next cue, as the Settings screen promises.
 * - Every channel call is posted to the main thread, which keeps channels free of locking and
 *   preserves the order cues were requested in. Callers never block.
 * - One failing channel (say, a broken TTS engine) is logged and never stops the others.
 */
@Singleton
class ChannelCueDispatcher @Inject constructor(
    private val channels: Set<@JvmSuppressWildcards CueChannel>,
    observeSettings: ObserveSettings,
    @PlayerEngineScope private val scope: CoroutineScope,
    @Dispatcher(RituneDispatchers.Main) private val mainDispatcher: CoroutineDispatcher,
) : CuePlayer {

    private val settings = observeSettings()
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    private var releaseJob: Job? = null

    override fun prepare() = onMain {
        releaseJob?.cancel()
        releaseJob = null
        channels.forEach { it.safely { prepare() } }
    }

    override fun play(cues: List<Cue>) {
        val allowed = cues.filter(settings.value::allows)
        if (allowed.isEmpty()) return
        onMain { allowed.forEach(::route) }
    }

    override fun countdown(secondsLeft: Int) {
        if (settings.value.countdownEnabled) play(CountdownCues)
    }

    override fun release() = onMain {
        releaseJob?.cancel()
        releaseJob = scope.launch(mainDispatcher) {
            delay(RELEASE_GRACE)
            channels.forEach { it.safely { release() } }
        }
    }

    private fun route(cue: Cue) {
        val channel = channels.firstOrNull { it.handles(cue) }
        if (channel == null) Log.w(TAG, "No channel for $cue") else channel.safely { play(cue) }
    }

    private fun onMain(block: () -> Unit) {
        scope.launch(mainDispatcher) { block() }
    }

    private inline fun CueChannel.safely(action: CueChannel.() -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            Log.e(TAG, "${this::class.simpleName} failed", e)
        }
    }

    private companion object {
        const val TAG = "ChannelCueDispatcher"
        val RELEASE_GRACE = 3.seconds
        val CountdownCues = listOf(
            Cue.Haptic(HapticPattern.TICK),
            Cue.Sound(SoundCue.TICK)
        )
    }
}
