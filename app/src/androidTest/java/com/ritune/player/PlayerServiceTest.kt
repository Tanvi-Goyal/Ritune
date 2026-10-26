package com.ritune.player

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.ritune.core.domain.ports.PlayerController
import com.ritune.core.domain.ports.StartResult
import com.ritune.core.domain.usecases.InitializeApp
import com.ritune.core.domain.usecases.player.StartRoutine
import com.ritune.core.models.DefaultCatalog
import com.ritune.core.models.player.PlayerState
import com.ritune.platform.android.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import javax.inject.Inject
import kotlin.test.assertEquals

/**
 * Checks the foreground service through NotificationManager instead of the notification shade:
 * the managed test device runs an ATD image, which has no System UI.
 */
@HiltAndroidTest
class PlayerServiceTest {

    private val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(hiltRule)
        .around(GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS))

    @Inject lateinit var initializeApp: InitializeApp
    @Inject lateinit var startRoutine: StartRoutine
    @Inject lateinit var player: PlayerController

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notifications = context.getSystemService(NotificationManager::class.java)

    private val pause = context.getString(R.string.notif_action_pause)
    private val resume = context.getString(R.string.notif_action_resume)
    private val skip = context.getString(R.string.notif_action_skip)

    @Before
    fun setUp(): Unit = runBlocking {
        hiltRule.inject()
        initializeApp()
        assertEquals(StartResult.Started, startRoutine(DefaultCatalog.morningStart.id))
    }

    @After
    fun tearDown(): Unit = runBlocking {
        player.stop()
        player.reset()
    }

    private fun Notification.actionTitles() = actions.orEmpty().map { it.title.toString() }

    private fun ongoing(): Notification? =
        notifications.activeNotifications.firstOrNull { it.isOngoing }?.notification

    private suspend fun awaitOngoing(match: (Notification?) -> Boolean): Notification? =
        withTimeout(TIMEOUT_MS) {
            while (!match(ongoing())) delay(POLL_MS)
            ongoing()
        }

    @Test
    fun runningRoutine_postsOngoingNotificationWithControls(): Unit = runBlocking {
        val notification = awaitOngoing { it?.actionTitles() == listOf(pause, skip) }

        assertEquals(listOf(pause, skip), notification?.actionTitles())
    }

    @Test
    fun pauseAction_pausesRunAndOffersResume(): Unit = runBlocking {
        val notification = awaitOngoing { it?.actionTitles()?.contains(pause) == true }

        notification!!.actions.first { it.title == pause }.actionIntent.send()

        withTimeout(TIMEOUT_MS) { player.state.first { it is PlayerState.Paused } }
        awaitOngoing { it?.actionTitles()?.contains(resume) == true }
    }

    @Test
    fun stoppingRun_removesOngoingNotification(): Unit = runBlocking {
        awaitOngoing { it != null }

        player.stop()

        awaitOngoing { it == null }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val POLL_MS = 50L
    }
}
