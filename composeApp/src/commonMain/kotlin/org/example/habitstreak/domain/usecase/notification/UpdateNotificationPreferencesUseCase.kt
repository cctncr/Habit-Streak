package org.example.habitstreak.domain.usecase.notification

import org.example.habitstreak.domain.repository.PreferencesRepository

/**
 * Use case to update notification sound and vibration preferences
 * Following Single Responsibility Principle - only updates preferences
 */
class UpdateNotificationPreferencesUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun execute(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        preferencesRepository.setSoundEnabled(soundEnabled)
        preferencesRepository.setVibrationEnabled(vibrationEnabled)
    }
}
