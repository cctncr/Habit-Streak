package org.example.habitstreak.domain.repository

import kotlinx.coroutines.flow.Flow
import org.example.habitstreak.domain.model.NotificationConfig

/**
 * Repository interface for notification configurations
 */
interface NotificationRepository {

    /**
     * Save or update notification configuration
     */
    suspend fun saveNotificationConfig(config: NotificationConfig)

    /**
     * Get notification configuration for a habit
     */
    suspend fun getNotificationConfig(habitId: String): NotificationConfig?

    /**
     * Get all notification configurations
     */
    suspend fun getAllNotificationConfigs(): List<NotificationConfig>

    /**
     * Update notification enabled status
     */
    suspend fun updateNotificationEnabled(habitId: String, enabled: Boolean)

    /**
     * Delete notification configuration
     */
    suspend fun deleteNotificationConfig(habitId: String)

    /**
     * Observe notification configuration changes
     */
    fun observeNotificationConfig(habitId: String): Flow<NotificationConfig?>

    /**
     * Observe all notification configurations
     */
    fun observeAllNotificationConfigs(): Flow<List<NotificationConfig>>

    /**
     * Disable all notifications
     */
    suspend fun disableAllNotifications()
}