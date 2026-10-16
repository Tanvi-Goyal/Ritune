package com.ritune.core.domain.di

import com.ritune.core.models.IdGenerator
import com.ritune.core.models.Moment
import com.ritune.core.models.Timekeeper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.Instant
import java.util.UUID

@InstallIn(SingletonComponent::class)
@Module
object RuntimeBindingsModule {

    @Provides
    @Singleton
    fun provideTimekeeper(): Timekeeper = object : Timekeeper {
        override fun now() = Moment(
            monoMillis = System.nanoTime() / 1_000_000L,
            wall = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
        )
    }

    @Provides
    @Singleton
    fun provideIdGenerator(): IdGenerator = IdGenerator {
        UUID.randomUUID().toString()
    }
}
