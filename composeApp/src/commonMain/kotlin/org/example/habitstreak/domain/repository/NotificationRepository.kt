package org.example.habitstreak.domain.repository

import kotlinx.coroutines.flow.Flow
import org.example.habitstreak.domain.model.NotificationConfig

interface NotificationRepository {
    suspend fun saveNotificationConfig(config: NotificationConfig)
    suspend fun getNotificationConfig(habitId: String): NotificationConfig?
    suspend fun getAllNotificationConfigs(): List<NotificationConfig>
    fun observeNotificationConfig(habitId: String): Flow<NotificationConfig?>
    suspend fun deleteNotificationConfig(habitId: String)
    suspend fun updateNotificationEnabled(habitId: String, enabled: Boolean)
}