package org.example.habitstreak.domain.service

import org.example.habitstreak.domain.model.NotificationConfig

/**
 * Platform-agnostic interface for scheduling notifications
 * Following Interface Segregation Principle - minimal interface
 */
interface NotificationScheduler {
    suspend fun scheduleNotification(config: NotificationConfig): Result<Unit>
    suspend fun cancelNotification(habitId: String): Result<Unit>
    suspend fun updateNotification(config: NotificationConfig): Result<Unit>
    suspend fun checkPermission(): Boolean
    suspend fun requestPermission(): Boolean
}
