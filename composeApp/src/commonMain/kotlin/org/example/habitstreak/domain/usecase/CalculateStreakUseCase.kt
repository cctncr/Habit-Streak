package org.example.habitstreak.domain.usecase

import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.usecase.util.UseCase
import org.example.habitstreak.domain.util.DateProvider

class CalculateStreakUseCase(
    private val habitRecordRepository: HabitRecordRepository,
    private val habitRepository: HabitRepository,
    private val dateProvider: DateProvider
) : UseCase<String, Result<CalculateStreakUseCase.StreakInfo>> {

    data class StreakInfo(
        val currentStreak: Int,
        val longestStreak: Int,
        val lastCompletedDate: LocalDate?
    )

    override suspend fun invoke(params: String): Result<StreakInfo> {
        return try {
            // Get habit info to know target count
            val habit = habitRepository.getHabitById(params).getOrNull()
                ?: return Result.success(StreakInfo(0, 0, null))

            val records = habitRecordRepository.getRecordsForHabit(params).getOrThrow()

            if (records.isEmpty()) {
                return Result.success(StreakInfo(0, 0, null))
            }

            // Only count FULLY completed days (completedCount >= targetCount)
            val fullyCompletedDates = records.filter { record ->
                record.completedCount >= habit.targetCount.coerceAtLeast(1)
            }.map { it.date }.sorted()

            if (fullyCompletedDates.isEmpty()) {
                return Result.success(StreakInfo(0, 0, null))
            }

            val today = dateProvider.today()
            val (currentStreak, longestStreak) = calculateStreaks(fullyCompletedDates, today)

            Result.success(StreakInfo(currentStreak, longestStreak, fullyCompletedDates.lastOrNull()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateStreaks(sortedDates: List<LocalDate>, today: LocalDate): Pair<Int, Int> {
        if (sortedDates.isEmpty()) return 0 to 0

        var longestStreak = 1
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
            0L -> tempStreak // Completed today
            1L -> tempStreak // Last completed yesterday
            else -> 0 // Streak broken
        }

        return currentStreak to longestStreak
    }
}