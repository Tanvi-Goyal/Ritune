package com.ritune.platform.android.di

import com.ritune.core.domain.ports.PlayerController
import com.ritune.core.domain.ports.RunRecorder
import com.ritune.core.domain.repositories.RunHistoryRepository
import com.ritune.core.domain.usecases.anchor.LogAnchor
import com.ritune.core.domain.usecases.player.DomainRunRecorder
import com.ritune.core.models.IdGenerator
import com.ritune.core.models.Timekeeper
import com.ritune.platform.android.EnginePlayerController
import com.ritune.platform.android.PlayerEngine
import com.ritune.platform.android.SystemTimekeeper
import com.ritune.platform.android.UuidGenerator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerEngineScope

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerEngineBindings {
    @Binds
    @Singleton
    abstract fun bindPlayerController(controller: EnginePlayerController): PlayerController

    @Binds
    @Singleton
    abstract fun bindsTimekeeper(impl: SystemTimekeeper): Timekeeper

    @Binds
    @Singleton
    abstract fun bindsIdGenerator(impl: UuidGenerator): IdGenerator

//    @Binds
//    abstract fun settings(impl: DataStoreSettingsRepository): SettingsRepository
//
//    @Binds
//    abstract fun onboarding(impl: DataStoreOnboardingStore): OnboardingStore
}

@Module
@InstallIn(SingletonComponent::class)
object PlayerEngineProviders {
    @Provides
    @Singleton
    @PlayerEngineScope
    fun providePlayerEngineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

//    @Provides
//    @Singleton
//    fun cueDispatcher(
//        haptic: HapticCueChannel,
//        speech: SpeechCueChannel,
//        sound: SoundCueChannel
//    ): CueDispatcher =
//        ChannelCueDispatcher(listOf(haptic, speech, sound))

    @Provides
    @Singleton
    fun provideRunRecorder(
        logAnchor: LogAnchor,
        runHistoryRepository: RunHistoryRepository
    ): RunRecorder =
        DomainRunRecorder(logAnchor, runHistoryRepository)


    @Provides
    @Singleton
    fun providePlayerEngine(
        timekeeper: Timekeeper,
        ids: IdGenerator,
        @PlayerEngineScope scope: CoroutineScope,
    ): PlayerEngine = PlayerEngine(timekeeper, ids, scope)

//    @Provides
//    @Singleton
//    fun playerEngine(
//        timekeeper: Timekeeper,
//        ids: IdGenerator,
//        cues: CueDispatcher,
//        recorder: RunRecorder,
//        @EngineScope scope: CoroutineScope,
//    ): PlayerEngine = PlayerEngine(
//        timekeeper = timekeeper,
//        ids = ids,
//        scheduler = CoroutineStepScheduler(timekeeper, scope),
//        cues = cues,
//        recorder = recorder,
//        scope = scope,
//        onError = { Log.e("PlayerEngine", "A cue or recording step failed", it) },
//    )
}
