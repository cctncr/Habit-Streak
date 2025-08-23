package org.example.habit_streak.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import org.example.habit_streak.domain.model.Habit
import org.example.habit_streak.domain.repository.HabitRecordRepository
import org.example.habit_streak.domain.repository.HabitRepository
import org.example.habit_streak.domain.usecase.util.FlowUseCase

class GetHabitsWithCompletionUseCase(
    private val habitRepository: HabitRepository,
    private val habitRecordRepository: HabitRecordRepository
) : FlowUseCase<LocalDate, List<GetHabitsWithCompletionUseCase.HabitWithCompletion>> {

    data class HabitWithCompletion(
        val habit: Habit,
        val isCompletedToday: Boolean,
        val completedCount: Int = 0
    )

    override fun invoke(params: LocalDate): Flow<List<HabitWithCompletion>> {
        return combine(
            habitRepository.observeActiveHabits(),
            habitRecordRepository.observeRecordsForDate(params)
        ) { habits, records ->
            habits.map { habit ->
                val record = records.find { it.habitId == habit.id }
                HabitWithCompletion(
                    habit = habit,
                    isCompletedToday = record != null,
                    completedCount = record?.completedCount ?: 0
                )
            }
        }
    }
}