package org.example.habit_streak.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.habit_streak.data.local.HabitDatabase
import org.example.habit_streak.data.mapper.toData
import org.example.habit_streak.data.mapper.toDomain
import org.example.habit_streak.domain.model.Habit
import org.example.habit_streak.domain.repository.HabitRepository
import java.util.UUID

class HabitRepositoryImpl(
    private val database: HabitDatabase
) : HabitRepository {

    private val queries = database.habitQueries

    override suspend fun createHabit(habit: Habit): Result<Habit> {
        return try {
            val habitWithId = if (habit.id.isEmpty()) {
                habit.copy(id = UUID.randomUUID().toString())
            } else habit

            val data = habitWithId.toData()
            queries.insert(data)
            Result.success(habitWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateHabit(habit: Habit): Result<Unit> {
        return try {
            with(habit) {
                val (frequencyType, frequencyData) = frequency.serialize()
                queries.update(
                    title = title,
                    description = description,
                    iconName = icon.name,
                    colorName = color.name,
                    frequencyType = frequencyType,
                    frequencyData = frequencyData,
                    reminderTime = reminderTime?.toString(),
                    isReminderEnabled = if (isReminderEnabled) 1L else 0L,
                    targetCount = targetCount.toLong(),
                    unit = unit,
                    isArchived = if (isArchived) 1L else 0L,
                    sortOrder = sortOrder.toLong(),
                    id = id
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> {
        return try {
            queries.delete(habitId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHabitById(habitId: String): Result<Habit?> {
        return try {
            val habit = queries.selectById(habitId).executeAsOneOrNull()?.toDomain()
            Result.success(habit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeAllHabits(): Flow<List<Habit>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun observeActiveHabits(): Flow<List<Habit>> {
        return queries.selectActive()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun observeHabitById(habitId: String): Flow<Habit?> {
        return queries.selectById(habitId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.firstOrNull()?.toDomain() }
    }
}