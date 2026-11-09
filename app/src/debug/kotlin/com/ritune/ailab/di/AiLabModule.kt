package com.ritune.ailab.di

import com.ritune.ailab.FirebaseLabChatClient
import com.ritune.ailab.LabChatClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
interface AiLabModule {
    @Binds
    fun bindLabChatClient(client: FirebaseLabChatClient): LabChatClient
}
