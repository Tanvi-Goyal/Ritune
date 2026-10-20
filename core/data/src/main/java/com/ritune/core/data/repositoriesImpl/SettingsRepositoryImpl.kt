package com.ritune.core.data.repositoriesImpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.ritune.core.datastore.di.SettingsDataStore
import com.ritune.core.domain.repositories.SettingsRepository
import com.ritune.core.models.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @SettingsDataStore private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SPEECH_ENABLED = booleanPreferencesKey("speech_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val COUNTDOWN_ENABLED = booleanPreferencesKey("countdown_enabled")
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
            .map { preferences ->
                val defaults = AppSettings()
                AppSettings(
                    notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED]
                        ?: defaults.notificationsEnabled,
                    darkModeEnabled = preferences[PreferencesKeys.DARK_MODE_ENABLED]
                        ?: defaults.darkModeEnabled,
                    hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED]
                        ?: defaults.hapticsEnabled,
                    speechEnabled = preferences[PreferencesKeys.SPEECH_ENABLED]
                        ?: defaults.speechEnabled,
                    soundEnabled = preferences[PreferencesKeys.SOUND_ENABLED]
                        ?: defaults.soundEnabled,
                    countdownEnabled = preferences[PreferencesKeys.COUNTDOWN_ENABLED]
                        ?: defaults.countdownEnabled,
                )
            }
    }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        val currentSettings = observe().first()
        val newSettings = transform(currentSettings)
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = newSettings.notificationsEnabled
            preferences[PreferencesKeys.DARK_MODE_ENABLED] = newSettings.darkModeEnabled
            preferences[PreferencesKeys.HAPTICS_ENABLED] = newSettings.hapticsEnabled
            preferences[PreferencesKeys.SPEECH_ENABLED] = newSettings.speechEnabled
            preferences[PreferencesKeys.SOUND_ENABLED] = newSettings.soundEnabled
            preferences[PreferencesKeys.COUNTDOWN_ENABLED] = newSettings.countdownEnabled
        }
    }
}
