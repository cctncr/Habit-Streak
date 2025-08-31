package org.example.habitstreak.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.habitstreak.domain.repository.PreferencesRepository
import java.util.prefs.Preferences

actual class PreferencesRepositoryImpl : PreferencesRepository {

    private val prefs: Preferences = Preferences.userNodeForPackage(PreferencesRepositoryImpl::class.java)

    companion object {
        const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val SOUND_ENABLED = "sound_enabled"
        const val VIBRATION_ENABLED = "vibration_enabled"
        const val THEME = "theme"
    }

    // Create flows for each preference - initialize with stored values
    private val _notificationsEnabled = MutableStateFlow(
        prefs.getBoolean(NOTIFICATIONS_ENABLED, true)
    )
    private val _soundEnabled = MutableStateFlow(
        prefs.getBoolean(SOUND_ENABLED, true)
    )
    private val _vibrationEnabled = MutableStateFlow(
        prefs.getBoolean(VIBRATION_ENABLED, true)
    )
    private val _theme = MutableStateFlow(
        prefs.get(THEME, "system")
    )

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        prefs.putBoolean(NOTIFICATIONS_ENABLED, enabled)
        prefs.flush()
        _notificationsEnabled.value = enabled
    }

    override fun isNotificationsEnabled(): Flow<Boolean> =
        _notificationsEnabled.asStateFlow()

    override suspend fun getNotificationsEnabled(): Boolean =
        _notificationsEnabled.value

    override suspend fun setSoundEnabled(enabled: Boolean) {
        prefs.putBoolean(SOUND_ENABLED, enabled)
        prefs.flush()
        _soundEnabled.value = enabled
    }

    override fun isSoundEnabled(): Flow<Boolean> =
        _soundEnabled.asStateFlow()

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        prefs.putBoolean(VIBRATION_ENABLED, enabled)
        prefs.flush()
        _vibrationEnabled.value = enabled
    }

    override fun isVibrationEnabled(): Flow<Boolean> =
        _vibrationEnabled.asStateFlow()

    override suspend fun setTheme(theme: String) {
        prefs.put(THEME, theme)
        prefs.flush()
        _theme.value = theme
    }

    override fun getTheme(): Flow<String> =
        _theme.asStateFlow()
}