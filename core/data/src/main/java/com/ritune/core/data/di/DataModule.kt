package com.ritune.core.data.di

import com.ritune.core.domain.initialization.AppInitializer
import com.ritune.core.domain.repositories.AnchorEventRepository
import com.ritune.core.domain.repositories.AnchorRepository
import com.ritune.core.domain.repositories.RoutineRepository
import com.ritune.core.domain.repositories.RunHistoryRepository
import com.ritune.core.data.DatabaseSeeder
import com.ritune.core.data.repositoriesImpl.AnchorEventRepositoryImpl
import com.ritune.core.data.repositoriesImpl.AnchorRepositoryImpl
import com.ritune.core.data.repositoriesImpl.RoutineRepositoryImpl
import com.ritune.core.data.repositoriesImpl.RunHistoryRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class DataModule {

    @Binds
    internal abstract fun bindsAppInitializer(
        databaseSeeder: DatabaseSeeder,
    ): AppInitializer

    @Binds
    internal abstract fun bindsAnchorRepository(
        anchorRepository: AnchorRepositoryImpl,
    ): AnchorRepository

    @Binds
    internal abstract fun bindsAnchorEventRepository(
        anchorEventRepository: AnchorEventRepositoryImpl,
    ): AnchorEventRepository

    @Binds
    internal abstract fun bindsRoutineRepository(
        routineRepository: RoutineRepositoryImpl,
    ): RoutineRepository

    @Binds
    internal abstract fun bindsRunRepository(
        runRepository: RunHistoryRepositoryImpl,
    ): RunHistoryRepository

}