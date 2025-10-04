package org.example.habitstreak.domain.service

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.model.NotificationPeriod
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.repository.NotificationRepository
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.core.error.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
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

            // Get existing config or create new one
            val existingConfig = notificationRepository.getNotificationConfig(habitId)

            // Create and save notification config with habit data
            val config = NotificationConfig(
                habitId = habitId,
                time = time,
                isEnabled = true,
                message = message ?: "Time to complete: ${habit.title}",
                period = existingConfig?.period ?: NotificationPeriod.EveryDay,
                habitFrequency = habit.frequency,
                habitCreatedAt = habit.createdAt
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
     * Cancel all scheduled notifications
     *
     * Note: This ONLY cancels the scheduled notifications in the system scheduler.
     * It does NOT modify the database notification configs (isEnabled stays unchanged).
     *
     * This is intentional because:
     * - When global notifications are disabled, we want to cancel scheduling but keep configs
     * - When global notifications are re-enabled, we want to restore from the saved configs
     * - This preserves user's individual habit notification settings
     */
    suspend fun cancelAllNotifications(): Result<Unit> {
        return try {
            scheduler.cancelAllNotifications()
            // DO NOT call notificationRepository.disableAllNotifications()
            // Keep database state intact so configs can be restored when global notifications are re-enabled
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update notification period for a habit
     */
    suspend fun updateNotificationPeriod(
        habitId: String,
        period: NotificationPeriod
    ): Result<Unit> {
        return try {
            // Get current config
            val config = notificationRepository.getNotificationConfig(habitId)
                ?: return Result.failure(NotificationNotFoundException(habitId))

            // Get habit details for scheduling
            val habit = habitRepository.getHabitById(habitId).getOrNull()
                ?: return Result.failure(HabitNotFoundException(habitId))

            // Update period in database
            notificationRepository.updateNotificationPeriod(habitId, period)

            // If notification is enabled, reschedule with new period and habit data
            if (config.isEnabled) {
                val updatedConfig = config.copy(
                    period = period,
                    habitFrequency = habit.frequency,
                    habitCreatedAt = habit.createdAt
                )
                scheduler.updateNotification(updatedConfig)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

