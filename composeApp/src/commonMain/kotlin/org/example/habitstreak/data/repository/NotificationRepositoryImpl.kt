package org.example.habitstreak.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime
import org.example.habitstreak.data.local.HabitDatabase
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.repository.NotificationRepository

class NotificationRepositoryImpl(
    private val database: HabitDatabase
) : NotificationRepository {

    override suspend fun saveNotificationConfig(config: NotificationConfig) = withContext(Dispatchers.IO) {
        database.habitQueries.transaction {
            val habit = database.habitQueries.selectById(config.habitId).executeAsOneOrNull()
            if (habit != null) {
                val (frequencyType, frequencyData) = deserializeFrequencyFromHabit(habit)
                database.habitQueries.update(
                    title = habit.title,
                    description = habit.description,
                    iconName = habit.iconName,
                    colorName = habit.colorName,
                    frequencyType = frequencyType,
                    frequencyData = frequencyData,
                    reminderTime = config.time.toString(),
                    isReminderEnabled = if (config.isEnabled) 1L else 0L,
                    targetCount = habit.targetCount,
                    unit = habit.unit,
                    isArchived = habit.isArchived,
                    sortOrder = habit.sortOrder,
                    id = config.habitId
                )
            }
        }
    }

    override suspend fun getNotificationConfig(habitId: String): NotificationConfig? =
        withContext(Dispatchers.IO) {
            database.habitQueries.selectById(habitId).executeAsOneOrNull()?.let { habit ->
                if (habit.reminderTime != null) {
                    try {
                        NotificationConfig(
                            habitId = habitId,
                            time = LocalTime.parse(habit.reminderTime),
                            isEnabled = habit.isReminderEnabled == 1L
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }
        }

    override suspend fun getAllNotificationConfigs(): List<NotificationConfig> =
        withContext(Dispatchers.IO) {
            database.habitQueries.selectAll().executeAsList()
                .mapNotNull { habit ->
                    if (habit.reminderTime != null && habit.isReminderEnabled == 1L) {
                        try {
                            NotificationConfig(
                                habitId = habit.id,
                                time = LocalTime.parse(habit.reminderTime),
                                isEnabled = true
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                }
        }

    override fun observeNotificationConfig(habitId: String): Flow<NotificationConfig?> = flow {
        val config = getNotificationConfig(habitId)
        emit(config)
    }

    override suspend fun deleteNotificationConfig(habitId: String) = withContext(Dispatchers.IO) {
        database.habitQueries.transaction {
            val habit = database.habitQueries.selectById(habitId).executeAsOneOrNull()
            if (habit != null) {
                val (frequencyType, frequencyData) = deserializeFrequencyFromHabit(habit)
                database.habitQueries.update(
                    title = habit.title,
                    description = habit.description,
                    iconName = habit.iconName,
                    colorName = habit.colorName,
                    frequencyType = frequencyType,
                    frequencyData = frequencyData,
                    reminderTime = null,
                    isReminderEnabled = 0L,
                    targetCount = habit.targetCount,
                    unit = habit.unit,
                    isArchived = habit.isArchived,
                    sortOrder = habit.sortOrder,
                    id = habitId
                )
            }
        }
    }

    override suspend fun updateNotificationEnabled(habitId: String, enabled: Boolean) =
        withContext(Dispatchers.IO) {
            database.habitQueries.transaction {
                val habit = database.habitQueries.selectById(habitId).executeAsOneOrNull()
                if (habit != null) {
                    val (frequencyType, frequencyData) = deserializeFrequencyFromHabit(habit)
                    database.habitQueries.update(
                        title = habit.title,
                        description = habit.description,
                        iconName = habit.iconName,
                        colorName = habit.colorName,
                        frequencyType = frequencyType,
                        frequencyData = frequencyData,
                        reminderTime = habit.reminderTime,
                        isReminderEnabled = if (enabled) 1L else 0L,
                        targetCount = habit.targetCount,
                        unit = habit.unit,
                        isArchived = habit.isArchived,
                        sortOrder = habit.sortOrder,
                        id = habitId
                    )
                }
            }
        }

    private fun deserializeFrequencyFromHabit(habit: org.example.habitstreak.data.local.Habit): Pair<String, String> {
        return habit.frequencyType to habit.frequencyData
    }
}