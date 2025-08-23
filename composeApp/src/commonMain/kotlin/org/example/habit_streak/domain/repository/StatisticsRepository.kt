package org.example.habit_streak.domain.repository

import kotlinx.coroutines.flow.Flow
import org.example.habit_streak.domain.model.HabitStatistics

interface StatisticsRepository {
    suspend fun getHabitStatistics(habitId: String): Result<HabitStatistics>
    suspend fun getAllStatistics(): Result<List<HabitStatistics>>
    fun observeHabitStatistics(habitId: String): Flow<HabitStatistics>
}