package com.ritune.platform.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.ritune.core.models.player.PlayerState
import com.ritune.core.models.RunSummary
import com.ritune.core.models.Session
import com.ritune.core.models.Timekeeper
import com.ritune.core.navigation.DeepLinks
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

/** Builds the ongoing routine notification and the "done" notification. */
@Singleton
class PlayerNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timekeeper: Timekeeper,
) {

    private val manager = context.getSystemService(
        NotificationManager::class.java
    )

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW, // silent: the cues are the sound
        ).apply {
            description = context.getString(R.string.notif_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * While running, the system draws the countdown itself (chronometer), so the notification is only updated
     * when something changes (step, pause, resume), never once a second.
     */
    fun ongoing(state: PlayerState): Notification {
        val builder = base().setContentIntent(
            open(DeepLinks.PLAYER, requestCode = 1)
        )
        when (state) {
            is PlayerState.Running -> {
                val now = timekeeper.now()
                val remainingMs =
                    (state.stepEndsAtMono - now.monoMillis)
                        .coerceAtLeast(0)
                builder
                    .setContentTitle(state.session.currentStep.title)
                    .setContentText(progressText(state.session))
                    .setShowWhen(true)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setWhen(now.wall.toEpochMilliseconds() + remainingMs)
                    .addAction(
                        0,
                        context.getString(R.string.notif_action_pause),
                        action(PlayerService.ACTION_PAUSE)
                    )
                    .addAction(0, skipLabel(state.session), action(PlayerService.ACTION_SKIP))
            }

            is PlayerState.Paused -> {
                builder
                    .setContentTitle(state.session.currentStep.title)
                    .setContentText(
                        context.getString(
                            R.string.notif_paused,
                            clock(state.remaining)
                        )
                    )
                    .setSubText(state.session.routine.name)
                    .setShowWhen(false)
                    .addAction(
                        0,
                        context.getString(R.string.notif_action_resume),
                        action(PlayerService.ACTION_RESUME)
                    )
                    .addAction(
                        0, skipLabel(state.session),
                        action(PlayerService.ACTION_SKIP)
                    )
            }

            else -> builder.setContentTitle(context.getString(R.string.notif_starting))
        }
        return builder.build()
    }

    fun completed(summary: RunSummary): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_ritune)
            .setContentTitle(
                context.getString(
                    R.string.notif_done_title,
                    summary.routineId
                )
            )
            .setContentText(context.getString(R.string.notif_done_text))
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(
                open(
                    "${DeepLinks.SUMMARY}/${summary.runId.value}",
                    requestCode = 2
                )
            )
            .build()

    private fun base() = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_ritune)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

    private fun progressText(session: Session) =
        context.getString(
            R.string.notif_step_progress,
            session.stepIndex + 1,
            session.routine.steps.size,
            session.routine.name
        )

    private fun skipLabel(session: Session) = context.getString(
        if (session.isLastStep) R.string.notif_action_finish
        else R.string.notif_action_skip,
    )

    /** An explicit, immutable intent to our own launcher activity: deep links navigate only. */
    private fun open(uri: String, requestCode: Int): PendingIntent {
        val launch =
            context.packageManager
                .getLaunchIntentForPackage(context.packageName) ?: Intent()
        val intent = Intent(launch)
            .setAction(Intent.ACTION_VIEW)
            .setData(uri.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun action(action: String): PendingIntent = PendingIntent.getService(
        context,
        action.hashCode(),
        Intent(context, PlayerService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun clock(duration: Duration): String {
        val total = (duration.inWholeMilliseconds + 999) / 1000
        return "%d:%02d".format(total / 60, total % 60)
    }

    companion object {
        const val CHANNEL_ID = "routine"
        const val ONGOING_ID = 1
        const val DONE_ID = 2
    }
}