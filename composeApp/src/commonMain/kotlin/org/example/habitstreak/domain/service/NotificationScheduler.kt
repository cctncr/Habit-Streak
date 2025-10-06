package org.example.habitstreak.domain.service

import kotlinx.datetime.Instant
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.NotificationConfig
import kotlin.time.ExperimentalTime

/**
 * Platform-agnostic interface for scheduling notifications
 */
@OptIn(ExperimentalTime::class)
interface NotificationScheduler {
    /**
     * Schedule a notification for a habit
     * @param config Notification configuration
     * @param habitFrequency Habit's frequency (for period calculation)
     * @param habitCreatedAt Habit's creation time (for period calculation)
     */
    suspend fun scheduleNotification(
        config: NotificationConfig,
        habitFrequency: HabitFrequency,
        habitCreatedAt: Instant
    ): Result<Unit>

    /**
     * Cancel a scheduled notification
     */
    suspend fun cancelNotification(habitId: String): Result<Unit>

    /**
     * Update an existing notification (cancel + reschedule)
     */
    suspend fun updateNotification(
        config: NotificationConfig,
        habitFrequency: HabitFrequency,
        habitCreatedAt: Instant
    ): Result<Unit>

    /**
     * Cancel all scheduled notifications
     */
    suspend fun cancelAllNotifications(): Result<Unit>

    /**
     * Check if a notification is scheduled
     */
    suspend fun isNotificationScheduled(habitId: String): Boolean
}