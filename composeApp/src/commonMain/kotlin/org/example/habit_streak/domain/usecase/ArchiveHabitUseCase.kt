package org.example.habit_streak.domain.usecase

import org.example.habit_streak.domain.repository.HabitRepository
import org.example.habit_streak.domain.usecase.util.UseCase

class ArchiveHabitUseCase(
    private val habitRepository: HabitRepository
) : UseCase<ArchiveHabitUseCase.Params, Result<Unit>> {

    data class Params(
        val habitId: String,
        val archive: Boolean
    )

    override suspend fun invoke(params: Params): Result<Unit> {
        return habitRepository.getHabitById(params.habitId).fold(
            onSuccess = { habit ->
                if (habit != null) {
                    habitRepository.updateHabit(habit.copy(isArchived = params.archive))
                } else {
                    Result.failure(IllegalArgumentException("Habit not found"))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }
}