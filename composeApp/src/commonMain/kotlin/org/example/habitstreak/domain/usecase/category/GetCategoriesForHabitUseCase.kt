package org.example.habitstreak.domain.usecase.category

import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.repository.CategoryRepository
import org.example.habitstreak.domain.repository.HabitRepository

/**
 * Use case for retrieving habits affected by a category operation.
 *
 * Implements SRP: Single responsibility for category-habit relationship queries.
 * Implements DIP: Presentation depends on abstraction (use case), not repository.
 */
class GetHabitsUsingCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val habitRepository: HabitRepository
) {
    suspend operator fun invoke(categoryId: String): Result<List<Habit>> {
        return categoryRepository.getHabitsUsingCategory(categoryId).fold(
            onSuccess = { habitIds ->
                val habits = habitIds.mapNotNull { habitId ->
                    habitRepository.getHabitById(habitId).getOrNull()
                }
                Result.success(habits)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}
