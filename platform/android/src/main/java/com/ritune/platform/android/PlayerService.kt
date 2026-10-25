package com.ritune.platform.android

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.ritune.core.domain.ports.PlayerController
import com.ritune.core.models.player.PlayerState
import com.ritune.core.models.RunOutcome
import com.ritune.platform.android.di.Dispatcher
import com.ritune.platform.android.di.RituneDispatchers
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the process alive and the notification current while a routine runs.
 * It does not own the run: the engine is a singleton and the UI drives it through
 * [PlayerController]. The service watches the engine's state and reacts: ongoing notification,
 * wake lock, and shutting itself down when the run ends.
 */
@AndroidEntryPoint
class PlayerService : Service() {

    @Inject
    lateinit var controller: PlayerController

    @Inject
    lateinit var notifications: PlayerNotifications

    @Inject
    @Dispatcher(RituneDispatchers.Main)
    lateinit var mainDispatcher: CoroutineDispatcher

    private lateinit var scope: CoroutineScope
    private val manager by lazy { getSystemService(NotificationManager::class.java) }
    private var observing: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(SupervisorJob() + mainDispatcher)
        notifications.ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> scope.launch { controller.pause() }
            ACTION_RESUME -> scope.launch { controller.resume() }
            ACTION_SKIP -> scope.launch { controller.skipStep() }
            else -> enterForeground()
        }
        // If the system kills the process mid-run, do not restart blindly; resuming a run is a separate feature.
        return START_NOT_STICKY
    }

    private fun enterForeground() {
        // Android requires startForeground soon after startForegroundService, so do it before anything else.
        startForegroundCompat(notifications.ongoing(controller.state.value))
        if (observing == null) {
            observing = scope.launch { controller.state.collect(::render) }
        }
    }

    private fun render(state: PlayerState) {
        when (state) {
            is PlayerState.Running -> {
                holdWakeLock() // timers must fire on time with the screen off
                manager.notify(PlayerNotifications.ONGOING_ID, notifications.ongoing(state))
            }

            is PlayerState.Paused -> {
                releaseWakeLock() // nothing is counting down
                manager.notify(PlayerNotifications.ONGOING_ID, notifications.ongoing(state))
            }

            is PlayerState.Finished -> {
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                if (state.summary.outcome == RunOutcome.COMPLETED) {
                    manager.notify(
                        PlayerNotifications.DONE_ID,
                        notifications.completed(state.summary)
                    )
                }
                stopSelf()
            }

            PlayerState.Idle -> {
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            PlayerNotifications.ONGOING_ID,
            notification,
            type
        )
    }

    private fun holdWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = power.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ritune:routine"
        ).apply {
            setReferenceCounted(false)
            acquire(MAX_RUN_MS) // a safety timeout: a bug can never keep the CPU awake for long
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PAUSE = "com.ritune.platform.action.PAUSE"
        const val ACTION_RESUME = "com.ritune.platform.action.RESUME"
        const val ACTION_SKIP = "com.ritune.platform.action.SKIP"

        /** Longest routine is 3 hours (RoutineValidator.MAX_TOTAL), plus slack for pauses. */
        private const val MAX_RUN_MS = 4L * 60 * 60 * 1000

        fun startIntent(context: Context) = Intent(
            context,
            PlayerService::class.java
        )
    }
}