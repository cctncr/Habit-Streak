package org.example.habitstreak.domain.usecase.notification

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.usecase.UseCase
import org.example.habitstreak.domain.util.HabitFrequencyUtils
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Use case to check if a habit is active on a specific date
 */
@OptIn(ExperimentalTime::class)
class CheckHabitActiveDayUseCase(
    private val habitRepository: HabitRepository
) : UseCase<CheckHabitActiveDayUseCase.Params, Result<Boolean>> {

    data class Params(
        val habitId: String,
        val date: LocalDate
    )

    override suspend fun invoke(params: Params): Result<Boolean> {
        return try {
            val habit = habitRepository.getHabitById(params.habitId).getOrNull()
                ?: return Result.failure(Exception("Habit not found"))

            // Check if habit is active on the given date
            val habitCreatedInstant = Instant.fromEpochMilliseconds(habit.createdAt.toEpochMilliseconds())
            val habitCreatedDate = habitCreatedInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date

            val isActive = HabitFrequencyUtils.isActiveOnDate(
                frequency = habit.frequency,
                date = params.date,
                habitCreatedAt = habitCreatedDate
            )

            Result.success(isActive)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}