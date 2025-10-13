package org.example.habitstreak.domain.usecase.habit

import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.repository.HabitRepository

class ReorderHabitsUseCase(
    private val habitRepository: HabitRepository
) {
    data class Params(
        val habits: List<Habit>,
        val fromIndex: Int,
        val toIndex: Int
    )

    suspend operator fun invoke(params: Params): Result<Unit> {
        return try {
            val reorderedHabits = params.habits.toMutableList()
            val movedHabit = reorderedHabits.removeAt(params.fromIndex)
            reorderedHabits.add(params.toIndex, movedHabit)

            val updatedHabits = reorderedHabits.mapIndexed { index, habit ->
                habit.id to index
            }.toMap()

            habitRepository.updateSortOrders(updatedHabits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
