package org.example.habitstreak.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import org.example.habitstreak.data.local.HabitDatabase
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.model.NotificationPeriod
import org.example.habitstreak.domain.repository.NotificationRepository
import kotlin.time.ExperimentalTime

/**
 * Implementation of NotificationRepository using SQLDelight
 * Following Single Responsibility - only manages notification data
 */
class NotificationRepositoryImpl(
    database: HabitDatabase
) : NotificationRepository {

    private val queries = database.notificationConfigQueries
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveNotificationConfig(config: NotificationConfig) {
        queries.insertOrReplace(
            habitId = config.habitId,
            time = config.time.toString(),
            isEnabled = if (config.isEnabled) 1L else 0L,
            message = config.message,
            period = json.encodeToString(NotificationPeriod.serializer(), config.period)
        )
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun getNotificationConfig(habitId: String): NotificationConfig? {
        return queries.getByHabitId(habitId).executeAsOneOrNull()?.let { entity ->
            NotificationConfig(
                habitId = entity.habitId,
                time = LocalTime.parse(entity.time),
                isEnabled = entity.isEnabled == 1L,
                message = entity.message,
                period = try {
                    json.decodeFromString(NotificationPeriod.serializer(), entity.period)
                } catch (e: Exception) {
                    NotificationPeriod.EveryDay // Default fallback
                }
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun getAllNotificationConfigs(): List<NotificationConfig> {
        return queries.getAll().executeAsList().map { entity ->
            NotificationConfig(
                habitId = entity.habitId,
                time = LocalTime.parse(entity.time),
                isEnabled = entity.isEnabled == 1L,
                message = entity.message,
                period = try {
                    json.decodeFromString(NotificationPeriod.serializer(), entity.period)
                } catch (e: Exception) {
                    NotificationPeriod.EveryDay // Default fallback
                }
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

    @OptIn(ExperimentalTime::class)
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
                        message = it.message,
                        period = try {
                            json.decodeFromString(NotificationPeriod.serializer(), it.period)
                        } catch (e: Exception) {
                            NotificationPeriod.EveryDay // Default fallback
                        }
                    )
                }
            }
    }

    @OptIn(ExperimentalTime::class)
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
                        message = entity.message,
                        period = try {
                            json.decodeFromString(NotificationPeriod.serializer(), entity.period)
                        } catch (e: Exception) {
                            NotificationPeriod.EveryDay // Default fallback
                        }
                    )
                }
            }
    }

    override suspend fun disableAllNotifications() {
        queries.disableAll()
    }

    override suspend fun updateNotificationPeriod(habitId: String, period: NotificationPeriod) {
        val periodJson = json.encodeToString(NotificationPeriod.serializer(), period)
        queries.updatePeriod(period = periodJson, habitId = habitId)
    }
}