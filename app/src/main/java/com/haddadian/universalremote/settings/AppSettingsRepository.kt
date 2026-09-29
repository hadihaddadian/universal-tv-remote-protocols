package com.haddadian.universalremote.settings

import android.content.Context

data class AppSettings(
    val darkMode: Boolean = false,
    val oneHandMode: Boolean = false,
    val hapticsEnabled: Boolean = true
)

/** Lightweight persistent settings implementation with no new dependency. */
class AppSettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        darkMode = prefs.getBoolean("dark_mode", false),
        oneHandMode = prefs.getBoolean("one_hand_mode", false),
        hapticsEnabled = prefs.getBoolean("haptics_enabled", true)
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putBoolean("dark_mode", settings.darkMode)
            .putBoolean("one_hand_mode", settings.oneHandMode)
            .putBoolean("haptics_enabled", settings.hapticsEnabled)
            .apply()
    }
}
