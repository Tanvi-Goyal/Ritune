package com.ritune.platform.ai.firebase.di

import com.ritune.core.domain.ports.CoachGateway
import com.ritune.platform.ai.firebase.FirebaseCoachGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AiFirebaseModule {
    @Binds
    fun bindCoachGateway(gateway: FirebaseCoachGateway): CoachGateway
}
