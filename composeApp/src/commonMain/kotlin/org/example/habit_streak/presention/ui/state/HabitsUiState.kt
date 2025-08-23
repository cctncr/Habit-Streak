package org.example.habit_streak.presention.ui.state

import org.example.habit_streak.domain.usecase.GetHabitsWithCompletionUseCase

data class HabitsUiState(
    val isLoading: Boolean = false,
    val habits: List<GetHabitsWithCompletionUseCase.HabitWithCompletion> = emptyList(),
    val streaks: Map<String, Int> = emptyMap(),
    val error: String? = null
)