package org.example.habitstreak.domain.repository

import kotlinx.coroutines.flow.Flow
import org.example.habitstreak.domain.model.Habit

interface HabitRepository {
    suspend fun createHabit(habit: Habit, categoryIds: List<String>): Result<Habit>
    suspend fun updateHabit(habit: Habit): Result<Unit>
    suspend fun deleteHabit(habitId: String): Result<Unit>
    suspend fun getHabitById(habitId: String): Result<Habit?>
    suspend fun updateSortOrders(habitSortOrders: Map<String, Int>): Result<Unit>
    fun observeActiveHabitsWithCategories(): Flow<List<Habit>>
    fun observeAllHabits(): Flow<List<Habit>>
    fun observeActiveHabits(): Flow<List<Habit>>
    fun observeArchivedHabits(): Flow<List<Habit>>
    fun observeHabitById(habitId: String): Flow<Habit?>
}