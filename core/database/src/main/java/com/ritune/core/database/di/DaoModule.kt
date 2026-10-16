package com.ritune.core.database.di

import com.ritune.core.database.RituneDatabase
import com.ritune.core.database.dao.AnchorDao
import com.ritune.core.database.dao.AnchorEventDao
import com.ritune.core.database.dao.RoutineDao
import com.ritune.core.database.dao.RunDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
internal object DaoModule {

    @Provides
    fun anchorDao(
        database: RituneDatabase,
    ) : AnchorDao = database.anchorDao()

    @Provides
    fun anchorEventDao(
        database: RituneDatabase,
    ) : AnchorEventDao = database.anchorEventDao()

    @Provides
    fun routineDao(
        database: RituneDatabase,
    ) : RoutineDao = database.routineDao()

    @Provides
    fun runDao(
        database: RituneDatabase,
    ) : RunDao = database.runDao()
}