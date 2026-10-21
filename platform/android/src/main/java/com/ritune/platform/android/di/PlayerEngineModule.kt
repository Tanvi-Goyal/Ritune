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
}

@Module
@InstallIn(SingletonComponent::class)
object PlayerEngineProviders {
    @Provides
    @Singleton
    @PlayerEngineScope
    fun providePlayerEngineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
}
