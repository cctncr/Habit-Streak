package org.example.habitstreak.domain.service

import org.example.habitstreak.domain.model.NotificationConfig

/**
 * Platform-agnostic interface for scheduling notifications
 */
interface NotificationScheduler {
    /**
     * Schedule a notification for a habit
     */
    suspend fun scheduleNotification(config: NotificationConfig): Result<Unit>

    /**
     * Cancel a scheduled notification
     */
    suspend fun cancelNotification(habitId: String): Result<Unit>

    /**
     * Update an existing notification
     */
    suspend fun updateNotification(config: NotificationConfig): Result<Unit>

    /**
     * Cancel all scheduled notifications
     */
    suspend fun cancelAllNotifications(): Result<Unit>

    /**
     * Check if a notification is scheduled
     */
    suspend fun isNotificationScheduled(habitId: String): Boolean
}