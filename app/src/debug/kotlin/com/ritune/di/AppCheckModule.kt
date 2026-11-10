package com.ritune.di

import com.ritune.ailab.installDebugAppCheck
import com.ritune.platform.ai.firebase.AppCheckSetup
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppCheckModule {
    @Provides
    fun appCheckSetup() = AppCheckSetup(::installDebugAppCheck)
}
