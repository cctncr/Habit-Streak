package org.example.habitstreak.domain.service

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.repository.NotificationRepository
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.core.error.*

class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val habitRepository: HabitRepository,
    private val scheduler: NotificationScheduler,
    private val preferencesRepository: PreferencesRepository
) {

    /**
     * Enable notification for a habit
     */
    suspend fun enableNotification(
        habitId: String,
        time: LocalTime,
        message: String? = null
    ): Result<Unit> {
        return try {
            // Check global notification setting first
            if (!preferencesRepository.getNotificationsEnabled()) {
                return Result.failure(NotificationsDisabledException())
            }

            // Get habit details
            val habit = habitRepository.getHabitById(habitId).getOrNull()
                ?: return Result.failure(HabitNotFoundException(habitId))

            // Create and save notification config
            val config = NotificationConfig(
                habitId = habitId,
                time = time,
                isEnabled = true,
                message = message ?: "Time to complete: ${habit.title}"
            )

            // Save to repository
            notificationRepository.saveNotificationConfig(config)

            // Schedule the notification
            scheduler.scheduleNotification(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Disable notification for a habit
     */
    suspend fun disableNotification(habitId: String): Result<Unit> {
        return try {
            notificationRepository.updateNotificationEnabled(habitId, false)
            scheduler.cancelNotification(habitId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update notification time for a habit
     */
    suspend fun updateNotificationTime(
        habitId: String,
        newTime: LocalTime
    ): Result<Unit> {
        return try {
            val config = notificationRepository.getNotificationConfig(habitId)
                ?: return Result.failure(NotificationNotFoundException(habitId))

            val updatedConfig = config.copy(time = newTime)
            notificationRepository.saveNotificationConfig(updatedConfig)

            if (config.isEnabled) {
                scheduler.updateNotification(updatedConfig)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync all notifications (e.g., after app restart)
     */
    suspend fun syncAllNotifications() {
        val configs = notificationRepository.getAllNotificationConfigs()
        configs.forEach { config ->
            if (config.isEnabled) {
                scheduler.scheduleNotification(config)
            }
        }
    }

    /**
     * Observe notification configuration for a habit
     */
    fun observeNotificationConfig(habitId: String): Flow<NotificationConfig?> {
        return notificationRepository.observeNotificationConfig(habitId)
    }

    /**
     * Cancel all notifications
     */
    suspend fun cancelAllNotifications(): Result<Unit> {
        return try {
            scheduler.cancelAllNotifications()
            notificationRepository.disableAllNotifications()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

