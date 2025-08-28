package org.example.habitstreak.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.usecase.util.FlowUseCase

class GetHabitsWithCompletionUseCase(
    private val habitRepository: HabitRepository,
    private val habitRecordRepository: HabitRecordRepository
) : FlowUseCase<LocalDate, List<GetHabitsWithCompletionUseCase.HabitWithCompletion>> {

    data class HabitWithCompletion(
        val habit: Habit,
        val isCompletedToday: Boolean,
        val completedCount: Int = 0,
        val todayRecord: HabitRecord? = null
    )

    override fun invoke(params: LocalDate): Flow<List<HabitWithCompletion>> {
        return combine(
            habitRepository.observeActiveHabits(),
            habitRecordRepository.observeRecordsForDate(params)
        ) { habits, records ->
            habits.map { habit ->
                val record = records.find { it.habitId == habit.id }
                val completedCount = record?.completedCount ?: 0
                val targetCount = habit.targetCount.coerceAtLeast(1)

                HabitWithCompletion(
                    habit = habit,
                    isCompletedToday = completedCount >= targetCount,
                    completedCount = completedCount,
                    todayRecord = record // Record bilgisi eklendi
                )
            }
        }
    }
}