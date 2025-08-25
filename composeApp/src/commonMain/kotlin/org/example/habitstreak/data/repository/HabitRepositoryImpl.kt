package org.example.habitstreak.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.habitstreak.data.local.HabitDatabase
import org.example.habitstreak.data.mapper.serialize
import org.example.habitstreak.data.mapper.toData
import org.example.habitstreak.data.mapper.toDomain
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.repository.HabitRepository
import kotlinx.coroutines.withContext
import org.example.habitstreak.core.util.UuidGenerator

class HabitRepositoryImpl(
    private val database: HabitDatabase
) : HabitRepository {

    private val queries = database.habitQueries

    override suspend fun createHabit(habit: Habit): Result<Habit> = withContext(Dispatchers.IO) {
        try {
            val habitWithId = if (habit.id.isEmpty()) {
                habit.copy(id = UuidGenerator.generateUUID())
            } else habit

            database.transaction {
                // Check for duplicate titles
                val existingHabits = queries.selectAll().executeAsList()
                val hasDuplicate = existingHabits.any {
                    it.title.equals(habitWithId.title, ignoreCase = true) &&
                            it.id != habitWithId.id &&
                            it.isArchived == 0L
                }

                if (hasDuplicate) {
                    throw IllegalArgumentException("A habit with this title already exists")
                }

                queries.insert(habitWithId.toData())
            }

            Result.success(habitWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateHabit(habit: Habit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                // Verify habit exists
                val existing = queries.selectById(habit.id).executeAsOneOrNull()
                    ?: throw IllegalArgumentException("Habit not found")

                // Check for duplicate titles (excluding current habit)
                val existingHabits = queries.selectAll().executeAsList()
                val hasDuplicate = existingHabits.any {
                    it.title.equals(habit.title, ignoreCase = true) &&
                            it.id != habit.id &&
                            it.isArchived == 0L
                }

                if (hasDuplicate) {
                    throw IllegalArgumentException("A habit with this title already exists")
                }

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
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                // Verify habit exists before deletion
                queries.selectById(habitId).executeAsOneOrNull()
                    ?: throw IllegalArgumentException("Habit not found")

                // Delete habit (cascade will handle records)
                queries.delete(habitId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHabitById(habitId: String): Result<Habit?> = withContext(Dispatchers.IO) {
        try {
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