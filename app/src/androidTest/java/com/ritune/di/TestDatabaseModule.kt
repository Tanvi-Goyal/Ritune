package com.ritune.di

import android.content.Context
import androidx.room3.Room
import com.ritune.core.database.RituneDatabase
import com.ritune.core.database.di.DatabaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideInMemoryDatabase(@ApplicationContext context: Context): RituneDatabase =
        Room.inMemoryDatabaseBuilder(context, RituneDatabase::class.java).build()
}
