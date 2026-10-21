package com.ritune.core.database.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context
import androidx.room3.Room.databaseBuilder
import com.ritune.core.database.RituneDatabase
import dagger.hilt.android.qualifiers.ApplicationContext

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun providesRituneDatabase(
        @ApplicationContext context: Context,
    ): RituneDatabase = databaseBuilder(
        context,
        RituneDatabase::class.java,
        "ritune-database",
    ).build()
}