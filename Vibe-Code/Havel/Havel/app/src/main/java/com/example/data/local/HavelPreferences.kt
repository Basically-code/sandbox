package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.havelDataStore by preferencesDataStore(name = "havel_prefs")

enum class ThemePreference(val key: String, val label: String) {
    SYSTEM("system", "Follow System"),
    DAY_ALABASTER("day", "Warm Alabaster (Day)"),
    DUSK_SLATE("dusk", "Muted Slate (Dusk)")
}

data class HavelSettings(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val lockScreenCaptureEnabled: Boolean = true,
    val wakeLockPadOnScreenOn: Boolean = true,
    val floatingBubbleEnabled: Boolean = false,
    val autoAiOrganizeEnabled: Boolean = true
)

class HavelPreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_preference")
    private val lockScreenKey = booleanPreferencesKey("lock_screen_capture_enabled")
    private val wakeLockPadKey = booleanPreferencesKey("wake_lock_pad_on_screen_on")
    private val bubbleKey = booleanPreferencesKey("floating_bubble_enabled")
    private val autoAiKey = booleanPreferencesKey("auto_ai_organize_enabled")

    val settingsFlow: Flow<HavelSettings> = context.havelDataStore.data.map { prefs ->
        val themeStr = prefs[themeKey] ?: ThemePreference.SYSTEM.key
        val theme = ThemePreference.entries.firstOrNull { it.key == themeStr } ?: ThemePreference.SYSTEM
        HavelSettings(
            themePreference = theme,
            lockScreenCaptureEnabled = prefs[lockScreenKey] ?: true,
            wakeLockPadOnScreenOn = prefs[wakeLockPadKey] ?: true,
            floatingBubbleEnabled = prefs[bubbleKey] ?: false,
            autoAiOrganizeEnabled = prefs[autoAiKey] ?: true
        )
    }

    suspend fun setThemePreference(preference: ThemePreference) {
        context.havelDataStore.edit { it[themeKey] = preference.key }
    }

    suspend fun setLockScreenCaptureEnabled(enabled: Boolean) {
        context.havelDataStore.edit { it[lockScreenKey] = enabled }
    }

    suspend fun setWakeLockPadOnScreenOn(enabled: Boolean) {
        context.havelDataStore.edit { it[wakeLockPadKey] = enabled }
    }

    suspend fun setFloatingBubbleEnabled(enabled: Boolean) {
        context.havelDataStore.edit { it[bubbleKey] = enabled }
    }

    suspend fun setAutoAiOrganizeEnabled(enabled: Boolean) {
        context.havelDataStore.edit { it[autoAiKey] = enabled }
    }
}
