package org.example.habit_streak.domain.usecase

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.habit_streak.domain.model.Habit
import org.example.habit_streak.domain.model.HabitColor
import org.example.habit_streak.domain.model.HabitFrequency
import org.example.habit_streak.domain.model.HabitIcon
import org.example.habit_streak.domain.repository.HabitRepository
import org.example.habit_streak.domain.usecase.util.UseCase
import kotlin.time.ExperimentalTime

class CreateHabitUseCase(
    private val habitRepository: HabitRepository
) : UseCase<CreateHabitUseCase.Params, Result<Habit>> {

    data class Params(
        val title: String,
        val description: String,
        val icon: HabitIcon,
        val color: HabitColor,
        val frequency: HabitFrequency,
        val reminderTime: LocalTime? = null,
        val targetCount: Int = 1,
        val unit: String = ""
    )

    @OptIn(ExperimentalTime::class)
    override suspend fun invoke(params: Params): Result<Habit> {
        // Validation logic
        if (params.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Habit title cannot be empty"))
        }

        if (params.targetCount < 1) {
            return Result.failure(IllegalArgumentException("Target count must be at least 1"))
        }

        val habit = Habit(
            title = params.title.trim(),
            description = params.description.trim(),
            icon = params.icon,
            color = params.color,
            frequency = params.frequency,
            reminderTime = params.reminderTime,
            isReminderEnabled = params.reminderTime != null,
            targetCount = params.targetCount,
            unit = params.unit,
            createdAt = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date // This would need proper date provider
        )

        return habitRepository.createHabit(habit)
    }
}