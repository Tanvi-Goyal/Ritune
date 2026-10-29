package com.ritune.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.ritune.core.datastore.di.DataStoreModule
import com.ritune.core.datastore.di.OnboardingDataStore
import com.ritune.core.datastore.di.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.UUID
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataStoreModule::class])
object TestDataStoreModule {

    @Provides
    @Singleton
    @SettingsDataStore
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        freshDataStore(context, "settings")

    @Provides
    @Singleton
    @OnboardingDataStore
    fun provideOnboardingDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        freshDataStore(context, "onboarding")

    private fun freshDataStore(context: Context, name: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = {
                File(
                    context.cacheDir,
                    "test-$name-${UUID.randomUUID()}.preferences_pb"
                )
            },
        )
}
