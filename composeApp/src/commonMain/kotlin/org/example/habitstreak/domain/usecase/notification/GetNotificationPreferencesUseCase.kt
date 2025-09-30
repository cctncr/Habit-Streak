package org.example.habitstreak.domain.usecase.notification

import kotlinx.coroutines.flow.first
import org.example.habitstreak.domain.repository.PreferencesRepository

/**
 * Use case to get notification sound and vibration preferences
 * Following Single Responsibility Principle - only reads preferences
 */
class GetNotificationPreferencesUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    data class Preferences(
        val soundEnabled: Boolean,
        val vibrationEnabled: Boolean
    )

    suspend fun execute(): Preferences {
        val soundEnabled = preferencesRepository.isSoundEnabled().first()
        val vibrationEnabled = preferencesRepository.isVibrationEnabled().first()
        return Preferences(soundEnabled, vibrationEnabled)
    }
}
