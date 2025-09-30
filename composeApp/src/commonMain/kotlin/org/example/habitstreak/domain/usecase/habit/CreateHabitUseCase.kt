package org.example.habitstreak.domain.usecase.habit

import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.Category
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitColor
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.HabitIcon
import org.example.habitstreak.domain.repository.CategoryRepository
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.service.HabitValidationService
import org.example.habitstreak.domain.usecase.UseCase
import org.example.habitstreak.domain.util.DateProvider
import kotlin.time.ExperimentalTime

class CreateHabitUseCase(
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository,
    private val dateProvider: DateProvider,
    private val validationService: HabitValidationService
) : UseCase<CreateHabitUseCase.Params, Result<Habit>> {

    data class Params(
        val title: String,
        val description: String,
        val icon: HabitIcon,
        val color: HabitColor,
        val frequency: HabitFrequency,
        val categories: List<Category> = emptyList(),
        val reminderTime: LocalTime? = null,
        val targetCount: Int = 1,
        val unit: String = ""
    )

    @OptIn(ExperimentalTime::class)
    override suspend fun invoke(params: Params): Result<Habit> {

        val validationResult = validationService.validateHabitCreation(
            title = params.title,
            description = params.description,
            targetCount = params.targetCount,
            unit = params.unit,
            frequency = params.frequency
        )

        if (!validationResult.isValid) {
            val errorMessage = validationResult.errors.joinToString(", ") { it.message }
            return Result.failure(IllegalArgumentException(errorMessage))
        }

        val habit = Habit(
            title = params.title.trim(),
            description = params.description.trim(),
            icon = params.icon,
            color = params.color,
            frequency = params.frequency,
            categories = params.categories,
            reminderTime = params.reminderTime?.toString(),
            isReminderEnabled = params.reminderTime != null,
            targetCount = params.targetCount,
            unit = params.unit,
            createdAt = dateProvider.now()
        )

        return habitRepository.createHabit(habit).fold(
            onSuccess = { createdHabit ->
                categoryRepository.updateHabitCategories(
                    createdHabit.id,
                    params.categories.map { it.id }
                ).fold(
                    onSuccess = {
                        params.categories.forEach { category ->
                            categoryRepository.incrementUsageCount(category.id)
                        }
                        Result.success(createdHabit)
                    },
                    onFailure = {
                        habitRepository.deleteHabit(createdHabit.id)
                        Result.failure(it)
                    }
                )
            },
            onFailure = { Result.failure(it) }
        )
    }
}