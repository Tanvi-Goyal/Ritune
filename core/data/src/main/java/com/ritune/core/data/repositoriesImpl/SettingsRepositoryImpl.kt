package com.ritune.core.data.repositoriesImpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ritune.core.datastore.di.SettingsDataStore
import com.ritune.core.domain.repositories.SettingsRepository
import com.ritune.core.models.AppSettings
import com.ritune.core.models.ThemeSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @SettingsDataStore private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SPEECH_ENABLED = booleanPreferencesKey("speech_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val COUNTDOWN_ENABLED = booleanPreferencesKey("countdown_enabled")
        val THEME = stringPreferencesKey("theme")
    }

    override fun observe(): Flow<AppSettings> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { it.toAppSettings() }
    }

    // Read and write inside one edit so concurrent updates can't overwrite each other.
    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            preferences.write(transform(preferences.toAppSettings()))
        }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            hapticsEnabled = this[PreferencesKeys.HAPTICS_ENABLED] ?: defaults.hapticsEnabled,
            speechEnabled = this[PreferencesKeys.SPEECH_ENABLED] ?: defaults.speechEnabled,
            soundEnabled = this[PreferencesKeys.SOUND_ENABLED] ?: defaults.soundEnabled,
            countdownEnabled = this[PreferencesKeys.COUNTDOWN_ENABLED] ?: defaults.countdownEnabled,
            theme = this[PreferencesKeys.THEME]
                ?.let { name -> ThemeSetting.entries.firstOrNull { it.name == name } }
                ?: defaults.theme,
        )
    }

    private fun MutablePreferences.write(settings: AppSettings) {
        this[PreferencesKeys.HAPTICS_ENABLED] = settings.hapticsEnabled
        this[PreferencesKeys.SPEECH_ENABLED] = settings.speechEnabled
        this[PreferencesKeys.SOUND_ENABLED] = settings.soundEnabled
        this[PreferencesKeys.COUNTDOWN_ENABLED] = settings.countdownEnabled
        this[PreferencesKeys.THEME] = settings.theme.name
    }
}
