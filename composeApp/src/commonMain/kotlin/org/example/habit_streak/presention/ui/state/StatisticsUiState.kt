package org.example.habit_streak.presention.ui.state

import org.example.habit_streak.domain.model.HabitStatistics

data class StatisticsUiState(
    val isLoading: Boolean = false,
    val statistics: List<HabitStatistics> = emptyList(),
    val selectedHabitStats: HabitStatistics? = null,
    val error: String? = null
)