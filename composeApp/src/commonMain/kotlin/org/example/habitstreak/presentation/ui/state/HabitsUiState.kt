package org.example.habitstreak.presentation.ui.state

import org.example.habitstreak.domain.usecase.GetHabitsWithCompletionUseCase

data class HabitsUiState(
    val isLoading: Boolean = false,
    val habits: List<GetHabitsWithCompletionUseCase.HabitWithCompletion> = emptyList(),
    val streaks: Map<String, Int> = emptyMap(),
    val error: String? = null
)