package org.example.habitstreak.presentation.ui.state

import org.example.habitstreak.domain.model.HabitStatistics

data class StatisticsUiState(
    val isLoading: Boolean = false,
    val statistics: List<HabitStatistics> = emptyList(),
    val selectedHabitStats: HabitStatistics? = null,
    val error: String? = null
)