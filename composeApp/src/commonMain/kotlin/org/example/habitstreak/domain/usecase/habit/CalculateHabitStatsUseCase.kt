package org.example.habitstreak.domain.usecase.habit

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.number
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.util.DateProvider

/**
 * Use case for calculating habit statistics.
 * Follows Single Responsibility Principle by handling only statistics calculations.
 */
class CalculateHabitStatsUseCase(
    private val dateProvider: DateProvider,
    private val calculateStreakUseCase: CalculateStreakUseCase
) {

    data class HabitStats(
        val currentStreak: Int = 0,
        val longestStreak: Int = 0,
        val totalDays: Int = 0,
        val completionRate: Double = 0.0,
        val averagePerDay: Double = 0.0,
        val thisWeekCount: Int = 0,
        val thisMonthCount: Int = 0,
        val lastCompleted: LocalDate? = null
    )

    suspend operator fun invoke(
        habitId: String,
        habit: Habit,
        records: List<HabitRecord>
    ): Result<HabitStats> = runCatching {
        val today = dateProvider.today()
        val fullyCompletedDates = records.filter { record ->
            record.completedCount >= habit.targetCount.coerceAtLeast(1)
        }.map { it.date }

        // Calculate streaks
        val streakResult = calculateStreakUseCase(habitId)
        val streakInfo = streakResult.getOrNull()

        // Calculate completion rate for last 30 days
        val last30Days = (0..29).map { today.minus(DatePeriod(days = it)) }
        val completedInLast30 = last30Days.count { it in fullyCompletedDates }
        val completionRate = (completedInLast30 / 30.0) * 100

        // Calculate this week and month stats
        val weekStart = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))
        val monthStart = LocalDate(today.year, today.month.number, 1)

        val thisWeekCount = fullyCompletedDates.count {
            it in weekStart..today
        }

        val thisMonthCount = fullyCompletedDates.count {
            it in monthStart..today
        }

        // Calculate average per day
        val averagePerDay = if (fullyCompletedDates.isNotEmpty()) {
            val daysSinceFirstRecord = records.minOfOrNull { it.date }?.let { firstDate ->
                (today.toEpochDays() - firstDate.toEpochDays()).toInt() + 1
            } ?: 1
            fullyCompletedDates.size.toDouble() / daysSinceFirstRecord
        } else 0.0

        HabitStats(
            currentStreak = streakInfo?.currentStreak ?: 0,
            longestStreak = streakInfo?.longestStreak ?: 0,
            totalDays = fullyCompletedDates.size,
            completionRate = completionRate,
            averagePerDay = averagePerDay,
            thisWeekCount = thisWeekCount,
            thisMonthCount = thisMonthCount,
            lastCompleted = fullyCompletedDates.maxOrNull()
        )
    }
}