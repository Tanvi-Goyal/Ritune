package com.ritune.platform.android

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.models.player.PlayerState
import com.ritune.core.navigation.DeepLinks
import com.ritune.core.testing.data.aRoutine
import com.ritune.core.testing.data.aRunSummary
import com.ritune.core.testing.data.aSession
import com.ritune.core.testing.data.aStep
import com.ritune.core.testing.data.testInstant
import com.ritune.core.testing.time.FakeTimekeeper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class PlayerNotificationsTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val notifications = PlayerNotifications(context, FakeTimekeeper(monoMillis = 10_000))

    private val routine = aRoutine(
        name = "Morning",
        steps = listOf(aStep(id = "a", title = "Stretch"), aStep(id = "b", title = "Breathe")),
    )

    private val Notification.title get() = extras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString()
    private val Notification.text get() = extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString()
    private val Notification.actionTitles get() = actions.map { it.title.toString() }
    private fun Notification.actionIntent(index: Int) = shadowOf(actions[index].actionIntent).savedIntent.action
    private val Notification.openedUri get() = shadowOf(contentIntent).savedIntent.dataString

    @Test
    fun ensureChannel_createsSilentChannel() {
        notifications.ensureChannel()

        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(PlayerNotifications.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
    }

    @Test
    fun ongoing_running_countsDownToStepEndWithPauseAndSkip() {
        val running = PlayerState.Running(aSession(routine), stepEndsAtMono = 55_000)

        val notification = notifications.ongoing(running)

        assertEquals("Stretch", notification.title)
        assertEquals("Step 1 of 2 · Morning", notification.text)
        assertTrue(notification.extras.getBoolean(NotificationCompat.EXTRA_SHOW_CHRONOMETER))
        assertEquals(testInstant.toEpochMilliseconds() + 45_000, notification.`when`)
        assertEquals(listOf("Pause", "Skip"), notification.actionTitles)
        assertEquals(PlayerService.ACTION_PAUSE, notification.actionIntent(0))
        assertEquals(PlayerService.ACTION_SKIP, notification.actionIntent(1))
        assertEquals(DeepLinks.PLAYER, notification.openedUri)
    }

    @Test
    fun ongoing_pausedOnLastStep_showsRemainingTimeWithResumeAndFinish() {
        val paused = PlayerState.Paused(aSession(routine, stepIndex = 1), remaining = 61.5.seconds)

        val notification = notifications.ongoing(paused)

        assertEquals("Breathe", notification.title)
        assertEquals("Paused · 1:02 left", notification.text)
        assertEquals(listOf("Resume", "Finish"), notification.actionTitles)
        assertEquals(PlayerService.ACTION_RESUME, notification.actionIntent(0))
    }

    @Test
    fun ongoing_idle_saysStarting() {
        assertEquals("Starting routine", notifications.ongoing(PlayerState.Idle).title)
    }

    @Test
    fun completed_opensRunSummary() {
        val notification = notifications.completed(aRunSummary(routine, runId = "run-7"))

        assertEquals("Tap to see the summary.", notification.text)
        assertEquals("${DeepLinks.SUMMARY}/run-7", notification.openedUri)
    }
}
