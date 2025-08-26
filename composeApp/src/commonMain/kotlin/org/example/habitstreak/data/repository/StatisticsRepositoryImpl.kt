package org.example.habitstreak.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.example.habitstreak.domain.model.HabitStatistics
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.util.DateProvider

class StatisticsRepositoryImpl(
    private val habitRepository: HabitRepository,
    private val recordRepository: HabitRecordRepository,
    private val dateProvider: DateProvider
) : StatisticsRepository {

    override suspend fun getHabitStatistics(habitId: String): Result<HabitStatistics> {
        return try {
            // Get habit info to know target count
            val habit = habitRepository.observeActiveHabits().first()
                .find { it.id == habitId }
                ?: return Result.failure(IllegalArgumentException("Habit not found"))

            val records = recordRepository.getRecordsForHabit(habitId).getOrThrow()

            // Only count fully completed days
            val fullyCompletedDates = records.filter { record ->
                record.completedCount >= habit.targetCount.coerceAtLeast(1)
            }.map { it.date }

            val stats = calculateStatistics(habitId, fullyCompletedDates, habit.targetCount)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllStatistics(): Result<List<HabitStatistics>> {
        return try {
            val habits = habitRepository.observeActiveHabits().first()
            val statistics = habits.mapNotNull { habit ->
                val records = recordRepository.getRecordsForHabit(habit.id).getOrNull() ?: emptyList()
                // Only count fully completed days
                val fullyCompletedDates = records.filter { record ->
                    record.completedCount >= habit.targetCount.coerceAtLeast(1)
                }.map { it.date }

                calculateStatistics(habit.id, fullyCompletedDates, habit.targetCount)
            }
            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeHabitStatistics(habitId: String): Flow<HabitStatistics> = flow {
        recordRepository.observeRecordsForHabit(habitId).collect { records ->
            val habit = habitRepository.observeActiveHabits().first()
                .find { it.id == habitId }

            if (habit != null) {
                // Only count fully completed days
                val fullyCompletedDates = records.filter { record ->
                    record.completedCount >= habit.targetCount.coerceAtLeast(1)
                }.map { it.date }

                emit(calculateStatistics(habitId, fullyCompletedDates, habit.targetCount))
            }
        }
    }

    private fun calculateStatistics(habitId: String, fullyCompletedDates: List<LocalDate>, targetCount: Int): HabitStatistics {
        if (fullyCompletedDates.isEmpty()) {
            return HabitStatistics(
                habitId = habitId,
                currentStreak = 0,
                longestStreak = 0,
                completionRate = 0f,
                totalCompletions = 0,
                lastCompletedDate = null
            )
        }

        val sortedDates = fullyCompletedDates.sorted()
        val today = dateProvider.today()

        // Calculate streaks - only using FULLY completed dates
        val (currentStreak, longestStreak) = calculateStreaks(sortedDates, today)

        // Calculate completion rate for last 30 days
        val thirtyDaysAgo = today.toEpochDays() - 30
        val recentDates = sortedDates.filter { it.toEpochDays() >= thirtyDaysAgo }
        val completionRate = if (recentDates.isNotEmpty()) {
            recentDates.size / 30f
        } else {
            0f
        }

        return HabitStatistics(
            habitId = habitId,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            completionRate = completionRate.coerceIn(0f, 1f),
            totalCompletions = fullyCompletedDates.size, // Only count fully completed days
            lastCompletedDate = sortedDates.lastOrNull()
        )
    }

    private fun calculateStreaks(sortedDates: List<LocalDate>, today: LocalDate): Pair<Int, Int> {
        if (sortedDates.isEmpty()) return 0 to 0

        var longestStreak = 1
        var currentStreakLength = 1
        var tempStreak = 1

        for (i in 1 until sortedDates.size) {
            val daysDiff = sortedDates[i].toEpochDays() - sortedDates[i - 1].toEpochDays()
            if (daysDiff == 1L) {
                tempStreak++
            } else {
                longestStreak = maxOf(longestStreak, tempStreak)
                tempStreak = 1
            }
        }
        longestStreak = maxOf(longestStreak, tempStreak)

        // Check if current streak is active
        val lastDate = sortedDates.last()
        val daysSinceLastCompletion = today.toEpochDays() - lastDate.toEpochDays()

        val currentStreak = when (daysSinceLastCompletion) {
            0L -> tempStreak
            1L -> tempStreak
            else -> 0
        }

        return currentStreak to longestStreak
    }
}