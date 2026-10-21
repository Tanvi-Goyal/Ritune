package com.ritune.platform.android


import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.ritune.core.domain.ports.CuePlayer
import com.ritune.core.domain.ports.PlayerController
import com.ritune.core.domain.ports.RunRecorder
import com.ritune.core.domain.ports.StartResult
import com.ritune.core.models.player.PlayerEffect
import com.ritune.core.models.player.PlayerState
import com.ritune.core.models.Routine
import com.ritune.platform.android.di.PlayerEngineScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the UI talks to. It drives the singleton engine directly and starts the foreground service so the
 * process survives while the routine runs. Calls come from the UI in the foreground, which is what lets the
 * service start on Android 12+.
 */
@Singleton
class EnginePlayerController @Inject constructor(
    private val engine: PlayerEngine,
    private val recorder: RunRecorder,
    private val cuePlayer: CuePlayer,
    @PlayerEngineScope private val scope: CoroutineScope,
    @ApplicationContext private val context: Context,
) : PlayerController {

    override val state: StateFlow<PlayerState> get() = engine.state

    init {
        scope.launch {
            engine.effects.collect { effect ->
                try {
                    when (effect) {
                        is PlayerEffect.RunStarted -> {
                            cuePlayer.prepare()
                            recorder.onRunStarted(effect.session)
                        }
                        is PlayerEffect.RunFinished -> {
                            recorder.onRunFinished(effect.summary)
                            cuePlayer.release()
                        }
                        is PlayerEffect.StepStarted -> Unit
                        is PlayerEffect.ScheduleStepEnd, PlayerEffect.CancelStepEnd -> Unit
                        is PlayerEffect.PlayCues -> cuePlayer.play(cues = effect.cues)
                        is PlayerEffect.CountdownTick ->
                            cuePlayer.countdown(secondsLeft = effect.secondsLeft)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: Exception) {
                    Log.e(
                        "EnginePlayerController",
                        "Failed to handle player effect: $effect",
                        exception
                    )
                }
            }
        }
    }

    override suspend fun start(routine: Routine): StartResult {
        if (!engine.start(routine)) return StartResult.AlreadyRunning
        ContextCompat.startForegroundService(
            context,
            PlayerService.startIntent(context)
        )
        return StartResult.Started
    }

    override suspend fun pause() = engine.pause()
    override suspend fun resume() = engine.resume()
    override suspend fun skipStep() = engine.skipStep()
    override suspend fun stop() = engine.stop()
    override suspend fun reset() = engine.reset()
}