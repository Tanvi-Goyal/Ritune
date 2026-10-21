package com.ritune.platform.android.di

import com.ritune.core.domain.ports.CuePlayer
import com.ritune.platform.android.cues.ChannelCueDispatcher
import com.ritune.platform.android.cues.CueChannel
import com.ritune.platform.android.cues.HapticCueChannel
import com.ritune.platform.android.cues.SoundCueChannel
import com.ritune.platform.android.cues.SpeechCueChannel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CueModule {

    @Binds
    @Singleton
    abstract fun bindCuePlayer(impl: ChannelCueDispatcher): CuePlayer

    @Binds
    @IntoSet
    abstract fun bindHapticChannel(impl: HapticCueChannel): CueChannel

    @Binds
    @IntoSet
    abstract fun bindSoundChannel(impl: SoundCueChannel): CueChannel

    @Binds
    @IntoSet
    abstract fun bindSpeechChannel(impl: SpeechCueChannel): CueChannel
}
