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
import kotlin.time.ExperimentalTime

class HabitRepositoryImpl(
    private val database: HabitDatabase
) : HabitRepository {

    private val queries = database.habitQueries

    @OptIn(ExperimentalTime::class)
    override suspend fun createHabit(habit: Habit): Result<Habit> = withContext(Dispatchers.IO) {
        try {
            val habitWithId = if (habit.id.isEmpty()) {
                habit.copy(id = UuidGenerator.generateUUID())
            } else habit

            database.transaction {
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
                val existing = queries.selectById(habit.id).executeAsOneOrNull()
                    ?: throw IllegalArgumentException("Habit not found")

                val existingHabits = queries.selectAll().executeAsList()
                val hasDuplicate = existingHabits.any {
                    it.title.equals(habit.title, ignoreCase = true) &&
                            it.id != habit.id &&
                            it.isArchived == 0L
                }

                if (hasDuplicate) {
                    throw IllegalArgumentException("A habit with this title already exists")
                }

                queries.update(
                    title = habit.title,
                    description = habit.description,
                    iconName = habit.icon.name,
                    colorName = habit.color.name,
                    frequencyType = habit.frequency.serialize().first,
                    frequencyData = habit.frequency.serialize().second,
                    reminderTime = habit.reminderTime,
                    isReminderEnabled = if (habit.isReminderEnabled) 1L else 0L,
                    targetCount = habit.targetCount.toLong(),
                    unit = habit.unit,
                    isArchived = if (habit.isArchived) 1L else 0L,
                    sortOrder = habit.sortOrder.toLong(),
                    id = habit.id
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                queries.selectById(habitId).executeAsOneOrNull()
                    ?: throw IllegalArgumentException("Habit not found")
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