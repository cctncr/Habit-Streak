package org.example.habitstreak.data.repositor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults
import org.example.habitstreak.domain.repository.PreferencesRepository

actual class PreferencesRepositoryImpl : PreferencesRepository {

    private val userDefaults = NSUserDefaults.standardUserDefaults

    companion object {
        const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val SOUND_ENABLED = "sound_enabled"
        const val VIBRATION_ENABLED = "vibration_enabled"
        const val THEME = "theme"
    }

    // Initialize with stored values or defaults
    init {
        // Set defaults if keys don't exist
        if (userDefaults.objectForKey(NOTIFICATIONS_ENABLED) == null) {
            userDefaults.setBool(true, NOTIFICATIONS_ENABLED)
        }
        if (userDefaults.objectForKey(SOUND_ENABLED) == null) {
            userDefaults.setBool(true, SOUND_ENABLED)
        }
        if (userDefaults.objectForKey(VIBRATION_ENABLED) == null) {
            userDefaults.setBool(true, VIBRATION_ENABLED)
        }
        if (userDefaults.objectForKey(THEME) == null) {
            userDefaults.setObject("system", THEME)
        }
        userDefaults.synchronize()
    }

    // Create flows for each preference
    private val _notificationsEnabled = MutableStateFlow(
        userDefaults.boolForKey(NOTIFICATIONS_ENABLED)
    )
    private val _soundEnabled = MutableStateFlow(
        userDefaults.boolForKey(SOUND_ENABLED)
    )
    private val _vibrationEnabled = MutableStateFlow(
        userDefaults.boolForKey(VIBRATION_ENABLED)
    )
    private val _theme = MutableStateFlow(
        userDefaults.objectForKey(THEME) as? String ?: "system"
    )

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, NOTIFICATIONS_ENABLED)
        userDefaults.synchronize()
        _notificationsEnabled.value = enabled
    }

    override fun isNotificationsEnabled(): Flow<Boolean> =
        _notificationsEnabled.asStateFlow()

    override suspend fun getNotificationsEnabled(): Boolean =
        _notificationsEnabled.value

    override suspend fun setSoundEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, SOUND_ENABLED)
        userDefaults.synchronize()
        _soundEnabled.value = enabled
    }

    override fun isSoundEnabled(): Flow<Boolean> =
        _soundEnabled.asStateFlow()

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, VIBRATION_ENABLED)
        userDefaults.synchronize()
        _vibrationEnabled.value = enabled
    }

    override fun isVibrationEnabled(): Flow<Boolean> =
        _vibrationEnabled.asStateFlow()

    override suspend fun setTheme(theme: String) {
        userDefaults.setObject(theme, THEME)
        userDefaults.synchronize()
        _theme.value = theme
    }

    override fun getTheme(): Flow<String> =
        _theme.asStateFlow()
}