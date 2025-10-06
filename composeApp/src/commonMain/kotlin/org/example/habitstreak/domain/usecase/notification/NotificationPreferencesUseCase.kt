package org.example.habitstreak.domain.usecase.notification

import kotlinx.coroutines.flow.first
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.NotificationService

/**
 * Unified use case for notification preferences (sound, vibration)
 * Consolidates: Get/UpdateNotificationPreferencesUseCase
 */
class NotificationPreferencesUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val notificationService: NotificationService
) {
    data class Preferences(
        val soundEnabled: Boolean,
        val vibrationEnabled: Boolean
    )

    suspend fun get(): Preferences {
        val soundEnabled = preferencesRepository.isSoundEnabled().first()
        val vibrationEnabled = preferencesRepository.isVibrationEnabled().first()
        return Preferences(soundEnabled, vibrationEnabled)
    }

    suspend fun updateSound(enabled: Boolean) {
        preferencesRepository.setSoundEnabled(enabled)
        notificationService.syncAllNotifications()
    }

    suspend fun updateVibration(enabled: Boolean) {
        preferencesRepository.setVibrationEnabled(enabled)
        notificationService.syncAllNotifications()
    }

    suspend fun updateBoth(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        preferencesRepository.setSoundEnabled(soundEnabled)
        preferencesRepository.setVibrationEnabled(vibrationEnabled)
        notificationService.syncAllNotifications()
    }
}
