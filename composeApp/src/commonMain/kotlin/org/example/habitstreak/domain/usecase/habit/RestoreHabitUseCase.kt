package org.example.habitstreak.domain.usecase.habit

import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.usecase.UseCase
import kotlin.time.ExperimentalTime

class RestoreHabitUseCase(
    private val habitRepository: HabitRepository
) : UseCase<String, Result<Unit>> {

    @OptIn(ExperimentalTime::class)
    override suspend fun invoke(params: String): Result<Unit> {
        return habitRepository.getHabitById(params).fold(
            onSuccess = { habit ->
                if (habit != null) {
                    habitRepository.updateHabit(habit.copy(isArchived = false))
                } else {
                    Result.failure(IllegalArgumentException("Habit not found"))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }
}