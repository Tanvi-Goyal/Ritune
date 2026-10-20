package com.ritune.core.models

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val speechEnabled: Boolean = false,
    val soundEnabled: Boolean = true,
    val countdownEnabled: Boolean = true,
    val theme: ThemeSetting = ThemeSetting.SYSTEM
)

enum class ThemeSetting {
    SYSTEM, LIGHT, DARK
}