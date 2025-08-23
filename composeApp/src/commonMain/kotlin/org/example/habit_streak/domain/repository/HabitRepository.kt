package org.example.habit_streak.domain.repository

import kotlinx.coroutines.flow.Flow
import org.example.habit_streak.domain.model.Habit

interface HabitRepository {
    suspend fun createHabit(habit: Habit): Result<Habit>
    suspend fun updateHabit(habit: Habit): Result<Unit>
    suspend fun deleteHabit(habitId: String): Result<Unit>
    suspend fun getHabitById(habitId: String): Result<Habit?>
    fun observeAllHabits(): Flow<List<Habit>>
    fun observeActiveHabits(): Flow<List<Habit>>
    fun observeHabitById(habitId: String): Flow<Habit?>
}