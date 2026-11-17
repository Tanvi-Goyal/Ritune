package com.ritune.profiling.di

import com.ritune.profiling.ProfileUploadScheduler
import com.ritune.profiling.ProfileUploader
import com.ritune.profiling.UnconfiguredProfileUploader
import com.ritune.profiling.WorkManagerProfileUploadScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ProfilingModule {
    @Binds
    fun scheduler(impl: WorkManagerProfileUploadScheduler): ProfileUploadScheduler

    @Binds
    fun uploader(impl: UnconfiguredProfileUploader): ProfileUploader
}
