package org.example.habitstreak.domain.usecase.habit

import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.usecase.UseCase
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.domain.util.HabitFrequencyUtils
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.habitstreak.domain.model.HabitFrequency
import kotlin.time.ExperimentalTime

class CalculateStreakUseCase(
    private val habitRepository: HabitRepository,
    private val habitRecordRepository: HabitRecordRepository,
    private val dateProvider: DateProvider
) : UseCase<String, Result<CalculateStreakUseCase.StreakInfo>> {

    data class StreakInfo(
        val currentStreak: Int,
        val longestStreak: Int
    )

    @OptIn(ExperimentalTime::class)
    override suspend fun invoke(params: String): Result<StreakInfo> {
        return try {
            val habitId = params
            val habitResult = habitRepository.getHabitById(habitId)
            if (habitResult.isFailure) {
                return Result.failure(habitResult.exceptionOrNull() ?: Exception("Failed to get habit"))
            }
            val habit = habitResult.getOrNull()
                ?: return Result.failure(Exception("Habit not found"))

            val recordsResult = habitRecordRepository.getRecordsForHabit(habitId)
            if (recordsResult.isFailure) {
                return Result.failure(recordsResult.exceptionOrNull() ?: Exception("Failed to get records"))
            }
            val records = recordsResult.getOrNull() ?: emptyList()
            val today = dateProvider.today()

            // Get fully completed dates - where completedCount >= targetCount
            val fullyCompletedDates = records.filter { record ->
                record.completedCount >= habit.targetCount.coerceAtLeast(1)
            }.map { it.date }.sorted()

            if (fullyCompletedDates.isEmpty()) {
                return Result.success(StreakInfo(currentStreak = 0, longestStreak = 0))
            }

            val habitCreatedAt = habit.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val (currentStreak, longestStreak) = calculateStreaks(fullyCompletedDates, today, habit.frequency, habitCreatedAt)

            Result.success(StreakInfo(currentStreak = currentStreak, longestStreak = longestStreak))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateStreaks(
        sortedDates: List<LocalDate>,
        today: LocalDate,
        frequency: HabitFrequency,
        habitCreatedAt: LocalDate
    ): Pair<Int, Int> {
        if (sortedDates.isEmpty()) return 0 to 0

        var longestStreak = 0
        var currentStreakFromEnd = 0

        // Calculate longest streak by checking frequency-aware consecutive completion
        var tempStreak = 1
        for (i in 1 until sortedDates.size) {
            val prevDate = sortedDates[i - 1]
            val currentDate = sortedDates[i]

            // Check if the streak is maintained by ensuring all active days between prev and current are covered
            if (isStreakMaintained(prevDate, currentDate, frequency, habitCreatedAt, sortedDates)) {
                tempStreak++
            } else {
                longestStreak = maxOf(longestStreak, tempStreak)
                tempStreak = 1
            }
        }
        longestStreak = maxOf(longestStreak, tempStreak)

        // Calculate current streak - check if streak is maintained from most recent completion to today
        val mostRecentDate = sortedDates.last()

        // Check if the streak from most recent completion to today is maintained
        if (isStreakMaintained(mostRecentDate, today, frequency, habitCreatedAt, sortedDates)) {
            currentStreakFromEnd = 1

            // Count backwards through consecutive completions
            for (i in sortedDates.size - 2 downTo 0) {
                val currentDate = sortedDates[i + 1]
                val prevDate = sortedDates[i]

                if (isStreakMaintained(prevDate, currentDate, frequency, habitCreatedAt, sortedDates)) {
                    currentStreakFromEnd++
                } else {
                    break
                }
            }
        }

        return currentStreakFromEnd to longestStreak
    }

    private fun isStreakMaintained(
        fromDate: LocalDate,
        toDate: LocalDate,
        frequency: HabitFrequency,
        habitCreatedAt: LocalDate,
        completedDates: List<LocalDate>
    ): Boolean {
        // For same date, always maintained
        if (fromDate == toDate) return true

        // Check all active days between fromDate (exclusive) and toDate (inclusive)
        var currentDate = LocalDate.fromEpochDays(fromDate.toEpochDays() + 1)

        while (currentDate <= toDate) {
            if (HabitFrequencyUtils.isActiveOnDate(frequency, currentDate, habitCreatedAt)) {
                // This day should have been completed to maintain the streak
                if (currentDate !in completedDates && currentDate != toDate) {
                    return false
                }
            }
            currentDate = LocalDate.fromEpochDays(currentDate.toEpochDays() + 1)
        }

        return true
    }
}