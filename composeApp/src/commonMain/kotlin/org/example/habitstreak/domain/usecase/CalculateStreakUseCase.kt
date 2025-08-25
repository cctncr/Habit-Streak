package org.example.habitstreak.domain.usecase

import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.usecase.util.UseCase
import org.example.habitstreak.domain.util.DateProvider

class CalculateStreakUseCase(
    private val habitRecordRepository: HabitRecordRepository,
    private val dateProvider: DateProvider
) : UseCase<String, Result<CalculateStreakUseCase.StreakInfo>> {

    data class StreakInfo(
        val currentStreak: Int,
        val longestStreak: Int,
        val lastCompletedDate: LocalDate?
    )

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
            val today = dateProvider.today()
            val lastDate = sortedDates.last()
            val daysSinceLastCompletion = today.toEpochDays() - lastDate.toEpochDays()

            currentStreak = when {
                daysSinceLastCompletion == 0L -> tempStreak // Completed today
                daysSinceLastCompletion == 1L -> tempStreak // Last completed yesterday
                else -> 0 // Streak broken
            }

            StreakInfo(currentStreak, longestStreak, lastDate)
        }
    }
}