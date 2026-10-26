package com.ritune.core.data.repositoriesImpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ritune.core.models.AppSettings
import com.ritune.core.models.ThemeSetting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private fun TestScope.newDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tmpFolder.root, "settings.preferences_pb") },
        )

    @Test
    fun observe_emptyStore_emitsDefaults() = runTest {
        val repository = SettingsRepositoryImpl(newDataStore())

        assertEquals(AppSettings(), repository.observe().first())
    }

    @Test
    fun update_persistsEveryField() = runTest {
        val repository = SettingsRepositoryImpl(newDataStore())
        val changed = AppSettings(
            hapticsEnabled = false,
            speechEnabled = true,
            soundEnabled = false,
            countdownEnabled = false,
            theme = ThemeSetting.DARK,
        )

        repository.update { changed }

        assertEquals(changed, repository.observe().first())
    }

    @Test
    fun observe_unknownStoredTheme_fallsBackToSystem() = runTest {
        val dataStore = newDataStore()
        dataStore.edit { it[stringPreferencesKey("theme")] = "SEPIA" }

        val settings = SettingsRepositoryImpl(dataStore).observe().first()

        assertEquals(ThemeSetting.SYSTEM, settings.theme)
    }

    @Test
    fun update_concurrentChanges_keepBoth() = runTest(UnconfinedTestDispatcher()) {
        val repository = SettingsRepositoryImpl(newDataStore())

        val speech = launch { repository.update { it.copy(speechEnabled = true) } }
        val theme = launch { repository.update { it.copy(theme = ThemeSetting.LIGHT) } }
        speech.join()
        theme.join()

        val settings = repository.observe().first()
        assertEquals(true, settings.speechEnabled)
        assertEquals(ThemeSetting.LIGHT, settings.theme)
    }
}
