package org.example.habitstreak.domain.usecase

import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.Category
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitColor
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.HabitIcon
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.usecase.util.UseCase
import org.example.habitstreak.domain.util.DateProvider
import kotlin.time.ExperimentalTime

class CreateHabitUseCase(
    private val habitRepository: HabitRepository,
    private val dateProvider: DateProvider
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
        if (params.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Habit başlığı boş olamaz"))
        }

        if (params.targetCount < 1) {
            return Result.failure(IllegalArgumentException("Hedef sayısı en az 1 olmalıdır"))
        }

        if (params.categories.isEmpty()) {
            return Result.failure(IllegalArgumentException("En az bir kategori seçilmelidir"))
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

        return habitRepository.createHabit(habit)
    }
}