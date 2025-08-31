package org.example.habitstreak.domain.service

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.repository.NotificationRepository
import org.example.habitstreak.domain.repository.PreferencesRepository

class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val habitRepository: HabitRepository,
    private val scheduler: NotificationScheduler,
    private val preferencesRepository: PreferencesRepository // Added
) {
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

            // Check permission
            if (!scheduler.checkPermission()) {
                val granted = scheduler.requestPermission()
                if (!granted) {
                    return Result.failure(NotificationPermissionDeniedException())
                }
            }

            val habit = habitRepository.getHabitById(habitId).getOrNull()
                ?: return Result.failure(HabitNotFoundException(habitId))

            val config = NotificationConfig(
                habitId = habitId,
                time = time,
                isEnabled = true,
                message = message ?: "Time to complete: ${habit.title}"
            )

            notificationRepository.saveNotificationConfig(config)
            scheduler.scheduleNotification(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disableNotification(habitId: String): Result<Unit> {
        return try {
            notificationRepository.updateNotificationEnabled(habitId, false)
            scheduler.cancelNotification(habitId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    suspend fun syncAllNotifications() {
        val configs = notificationRepository.getAllNotificationConfigs()
        configs.forEach { config ->
            if (config.isEnabled) {
                scheduler.scheduleNotification(config)
            }
        }
    }

    fun observeNotificationConfig(habitId: String): Flow<NotificationConfig?> {
        return notificationRepository.observeNotificationConfig(habitId)
    }
}

// Exception classes
class NotificationPermissionDeniedException : Exception("Notification permission denied")
class HabitNotFoundException(habitId: String) : Exception("Habit not found: $habitId")
class NotificationNotFoundException(habitId: String) :
    Exception("Notification config not found for habit: $habitId")
class NotificationsDisabledException : Exception("Notifications are globally disabled")