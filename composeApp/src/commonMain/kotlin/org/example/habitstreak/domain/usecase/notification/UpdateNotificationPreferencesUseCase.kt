package org.example.habitstreak.domain.usecase.notification

import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.NotificationService

/**
 * Use case to update notification sound and vibration preferences
 * and sync all active notifications with new settings
 * Following Single Responsibility Principle - only updates preferences and syncs
 */
class UpdateNotificationPreferencesUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val notificationService: NotificationService
) {
    /**
     * Update sound preference and sync all notifications
     */
    suspend fun updateSound(enabled: Boolean) {
        preferencesRepository.setSoundEnabled(enabled)
        // Re-schedule all active notifications with new sound preference
        notificationService.syncAllNotifications()
    }

    /**
     * Update vibration preference and sync all notifications
     */
    suspend fun updateVibration(enabled: Boolean) {
        preferencesRepository.setVibrationEnabled(enabled)
        // Re-schedule all active notifications with new vibration preference
        notificationService.syncAllNotifications()
    }

    /**
     * Update both sound and vibration preferences and sync all notifications
     * More efficient than calling updateSound and updateVibration separately
     */
    suspend fun updateBoth(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        preferencesRepository.setSoundEnabled(soundEnabled)
        preferencesRepository.setVibrationEnabled(vibrationEnabled)
        // Re-schedule all active notifications once with both new preferences
        notificationService.syncAllNotifications()
    }
}
