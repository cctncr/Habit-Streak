package org.example.habitstreak.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.habitstreak.data.local.HabitDatabase
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.repository.NotificationRepository
import kotlinx.datetime.LocalTime

/**
 * Implementation of NotificationRepository using SQLDelight
 * Following Single Responsibility - only manages notification data
 */
class NotificationRepositoryImpl(
    private val database: HabitDatabase
) : NotificationRepository {

    private val queries = database.notificationConfigQueries

    override suspend fun saveNotificationConfig(config: NotificationConfig) {
        queries.insertOrReplace(
            habitId = config.habitId,
            time = config.time.toString(),
            isEnabled = if (config.isEnabled) 1L else 0L,
            message = config.message
        )
    }

    override suspend fun getNotificationConfig(habitId: String): NotificationConfig? {
        return queries.getByHabitId(habitId).executeAsOneOrNull()?.let { entity ->
            NotificationConfig(
                habitId = entity.habitId,
                time = LocalTime.parse(entity.time),
                isEnabled = entity.isEnabled == 1L,
                message = entity.message
            )
        }
    }

    override suspend fun getAllNotificationConfigs(): List<NotificationConfig> {
        return queries.getAll().executeAsList().map { entity ->
            NotificationConfig(
                habitId = entity.habitId,
                time = LocalTime.parse(entity.time),
                isEnabled = entity.isEnabled == 1L,
                message = entity.message
            )
        }
    }

    override suspend fun updateNotificationEnabled(habitId: String, enabled: Boolean) {
        queries.updateEnabled(
            isEnabled = if (enabled) 1L else 0L,
            habitId = habitId
        )
    }

    override suspend fun deleteNotificationConfig(habitId: String) {
        queries.delete(habitId)
    }

    override fun observeNotificationConfig(habitId: String): Flow<NotificationConfig?> {
        return queries.getByHabitId(habitId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { entity ->
                entity?.let {
                    NotificationConfig(
                        habitId = it.habitId,
                        time = LocalTime.parse(it.time),
                        isEnabled = it.isEnabled == 1L,
                        message = it.message
                    )
                }
            }
    }

    override fun observeAllNotificationConfigs(): Flow<List<NotificationConfig>> {
        return queries.getAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { entity ->
                    NotificationConfig(
                        habitId = entity.habitId,
                        time = LocalTime.parse(entity.time),
                        isEnabled = entity.isEnabled == 1L,
                        message = entity.message
                    )
                }
            }
    }

    override suspend fun disableAllNotifications() {
        queries.disableAll()
    }
}