package org.example.habitstreak.presentation.ui.state

import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.usecase.habit.GetHabitsWithCompletionUseCase

data class HabitsUiState(
    val isLoading: Boolean = false,
    val habits: List<GetHabitsWithCompletionUseCase.HabitWithCompletion> = emptyList(),
    val streaks: Map<String, Int> = emptyMap(),
    val completionHistories: Map<String, Map<LocalDate, Float>> = emptyMap(),
    val allRecords: List<HabitRecord> = emptyList(),
    val error: String? = null
)