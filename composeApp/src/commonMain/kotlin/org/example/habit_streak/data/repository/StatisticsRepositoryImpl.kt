package org.example.habit_streak.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.habit_streak.domain.model.HabitStatistics
import org.example.habit_streak.domain.repository.HabitRecordRepository
import org.example.habit_streak.domain.repository.HabitRepository
import org.example.habit_streak.domain.repository.StatisticsRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class StatisticsRepositoryImpl(
    private val habitRepository: HabitRepository,
    private val recordRepository: HabitRecordRepository
) : StatisticsRepository {

    override suspend fun getHabitStatistics(habitId: String): Result<HabitStatistics> {
        return try {
            val records = recordRepository.getRecordsForHabit(habitId).getOrThrow()
            val stats = calculateStatistics(habitId, records.map { it.date })
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllStatistics(): Result<List<HabitStatistics>> {
        return try {
            val habits = habitRepository.observeActiveHabits().first()
            val statistics = habits.map { habit ->
                getHabitStatistics(habit.id).getOrThrow()
            }
            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeHabitStatistics(habitId: String): Flow<HabitStatistics> = flow {
        recordRepository.observeRecordsForHabit(habitId).collect { records ->
            emit(calculateStatistics(habitId, records.map { it.date }))
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun calculateStatistics(habitId: String, dates: List<LocalDate>): HabitStatistics {
        if (dates.isEmpty()) {
            return HabitStatistics(
                habitId = habitId,
                currentStreak = 0,
                longestStreak = 0,
                completionRate = 0f,
                totalCompletions = 0,
                lastCompletedDate = null
            )
        }

        val sortedDates = dates.sorted()
        var currentStreak = 0
        var longestStreak = 0
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

        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        val lastDate = sortedDates.last()
        currentStreak = if (lastDate == today || lastDate.toEpochDays() == today.toEpochDays() - 1) {
            tempStreak
        } else {
            0
        }

        val thirtyDaysAgo = today.toEpochDays() - 30
        val recentDates = sortedDates.filter { it.toEpochDays() >= thirtyDaysAgo }
        val completionRate = recentDates.size / 30f

        return HabitStatistics(
            habitId = habitId,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            completionRate = completionRate.coerceIn(0f, 1f),
            totalCompletions = dates.size,
            lastCompletedDate = lastDate
        )
    }
}