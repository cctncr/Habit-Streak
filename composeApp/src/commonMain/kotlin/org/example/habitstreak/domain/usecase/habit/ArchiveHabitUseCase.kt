package org.example.habitstreak.domain.usecase.habit

import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.usecase.UseCase
import kotlin.time.ExperimentalTime

class ArchiveHabitUseCase(
    private val habitRepository: HabitRepository
) : UseCase<ArchiveHabitUseCase.Params, Result<Unit>> {

    data class Params(
        val habitId: String,
        val archive: Boolean
    )

    @OptIn(ExperimentalTime::class)
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