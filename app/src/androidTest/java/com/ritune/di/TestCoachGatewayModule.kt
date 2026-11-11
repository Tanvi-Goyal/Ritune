package com.ritune.di

import com.ritune.core.domain.ports.CoachGateway
import com.ritune.core.testing.ports.FakeCoachGateway
import com.ritune.platform.ai.firebase.di.AiFirebaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [AiFirebaseModule::class])
object TestCoachGatewayModule {

    @Provides
    @Singleton
    fun provideFakeCoachGateway() = FakeCoachGateway()

    @Provides
    fun provideCoachGateway(fake: FakeCoachGateway): CoachGateway = fake
}
