package com.ritune.platform.android.cues

import com.ritune.core.domain.usecases.settings.ObserveSettings
import com.ritune.core.models.AppSettings
import com.ritune.core.models.Cue
import com.ritune.core.models.HapticPattern
import com.ritune.core.models.SoundCue
import com.ritune.core.testing.repository.FakeSettingsRepository
import com.ritune.platform.android.FakeLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelCueDispatcherTest {

    private class FakeCueChannel(private val type: KClass<out Cue>, private val fails: Boolean = false) : CueChannel {
        val played = mutableListOf<Cue>()
        var prepared = 0
        var released = 0

        override fun handles(cue: Cue) = type.isInstance(cue)
        override fun play(cue: Cue) {
            if (fails) error("channel broken")
            played += cue
        }
        override fun prepare() {
            prepared++
        }
        override fun release() {
            released++
        }
    }

    private val haptic = Cue.Haptic(HapticPattern.STEP_CHANGE)
    private val sound = Cue.Sound(SoundCue.SOFT_BELL)
    private val speech = Cue.Speech("Stretch")

    private val settings = FakeSettingsRepository(AppSettings(speechEnabled = false))
    private val logger = FakeLogger()

    private fun TestScope.newDispatcher(vararg channels: CueChannel): ChannelCueDispatcher {
        val dispatcher = ChannelCueDispatcher(
            channels = linkedSetOf(*channels),
            observeSettings = ObserveSettings(settings),
            scope = backgroundScope,
            mainDispatcher = StandardTestDispatcher(testScheduler),
            logger = logger,
        )
        runCurrent()
        return dispatcher
    }

    @Test
    fun play_routesAllowedCuesToTheirChannel() = runTest {
        val haptics = FakeCueChannel(Cue.Haptic::class)
        val speaker = FakeCueChannel(Cue.Speech::class)
        val dispatcher = newDispatcher(haptics, speaker)

        dispatcher.play(listOf(haptic, speech))
        runCurrent()

        assertEquals(listOf<Cue>(haptic), haptics.played)
        assertEquals(emptyList(), speaker.played)
    }

    @Test
    fun play_settingChangedMidRun_appliesToNextCue() = runTest {
        val haptics = FakeCueChannel(Cue.Haptic::class)
        val dispatcher = newDispatcher(haptics)

        settings.settings.update { it.copy(hapticsEnabled = false) }
        runCurrent()
        dispatcher.play(listOf(haptic))
        runCurrent()

        assertEquals(emptyList(), haptics.played)
    }

    @Test
    fun countdown_followsCountdownSetting() = runTest {
        val haptics = FakeCueChannel(Cue.Haptic::class)
        val bells = FakeCueChannel(Cue.Sound::class)
        val dispatcher = newDispatcher(haptics, bells)

        dispatcher.countdown(3)
        runCurrent()
        settings.settings.update { it.copy(countdownEnabled = false) }
        runCurrent()
        dispatcher.countdown(2)
        runCurrent()

        assertEquals(listOf<Cue>(Cue.Haptic(HapticPattern.TICK)), haptics.played)
        assertEquals(listOf<Cue>(Cue.Sound(SoundCue.TICK)), bells.played)
    }

    @Test
    fun release_waitsGracePeriodBeforeReleasingChannels() = runTest {
        val channel = FakeCueChannel(Cue.Haptic::class)
        val dispatcher = newDispatcher(channel)

        dispatcher.release()
        advanceTimeBy(3.seconds)
        assertEquals(0, channel.released)

        runCurrent()
        assertEquals(1, channel.released)
    }

    @Test
    fun prepare_duringGracePeriod_cancelsRelease() = runTest {
        val channel = FakeCueChannel(Cue.Haptic::class)
        val dispatcher = newDispatcher(channel)

        dispatcher.release()
        advanceTimeBy(1.seconds)
        dispatcher.prepare()
        advanceTimeBy(5.seconds)

        assertEquals(1, channel.prepared)
        assertEquals(0, channel.released)
    }

    @Test
    fun play_failingChannel_isLoggedAndOthersStillPlay() = runTest {
        val broken = FakeCueChannel(Cue.Haptic::class, fails = true)
        val bells = FakeCueChannel(Cue.Sound::class)
        val dispatcher = newDispatcher(broken, bells)

        dispatcher.play(listOf(haptic, sound))
        runCurrent()

        assertEquals(listOf<Cue>(sound), bells.played)
        assertEquals(1, logger.errors.size)
    }

    @Test
    fun play_cueWithoutChannel_logsWarning() = runTest {
        val dispatcher = newDispatcher(FakeCueChannel(Cue.Haptic::class))

        dispatcher.play(listOf(sound))
        runCurrent()

        assertEquals(1, logger.warnings.size)
    }
}
