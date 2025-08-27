package org.example.habitstreak.presentation.ui.state

import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel.FilteredStats
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel.HabitStats
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel.YearMonth

data class HabitDetailUiState(
    val habit: Habit? = null,
    val records: List<HabitRecord> = emptyList(),
    val recentRecords: List<HabitRecord> = emptyList(),
    val statistics: HabitStats? = null,
    val filteredStatistics: FilteredStats? = null,
    val currentMonth: YearMonth = YearMonth(2024, 1),
    val isLoading: Boolean = false,
    val error: String? = null
)