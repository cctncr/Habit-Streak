package org.example.habit_streak.domain.usecase

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.habit_streak.domain.repository.HabitRecordRepository
import org.example.habit_streak.domain.usecase.util.UseCase
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CalculateStreakUseCase(
    private val habitRecordRepository: HabitRecordRepository
) : UseCase<String, Result<CalculateStreakUseCase.StreakInfo>> {

    data class StreakInfo(
        val currentStreak: Int,
        val longestStreak: Int,
        val lastCompletedDate: LocalDate?
    )

    @OptIn(ExperimentalTime::class)
    override suspend fun invoke(params: String): Result<StreakInfo> {
        return habitRecordRepository.getRecordsForHabit(params).map { records ->
            if (records.isEmpty()) {
                return@map StreakInfo(0, 0, null)
            }

            val sortedDates = records.map { it.date }.sorted()
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

            // Check if streak is still active
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date // This would need proper date provider
            val lastDate = sortedDates.last()
            currentStreak = if (lastDate == today || lastDate.toEpochDays() == today.toEpochDays() - 1) {
                tempStreak
            } else {
                0
            }

            StreakInfo(currentStreak, longestStreak, lastDate)
        }
    }
}