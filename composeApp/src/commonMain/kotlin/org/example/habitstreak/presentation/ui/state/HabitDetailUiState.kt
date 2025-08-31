package org.example.habitstreak.presentation.ui.state

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.presentation.model.YearMonth
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel.HabitStats

data class HabitDetailUiState(
    val habit: Habit? = null,
    val records: List<HabitRecord> = emptyList(),
    val statistics: HabitStats? = null,
    val filteredStatistics: HabitStats? = null,
    val isLoading: Boolean = true,
    val selectedDate: LocalDate? = null,
    val selectedMonth: YearMonth? = null,
    val notificationConfig: NotificationConfig? = null,
    val isNotificationEnabled: Boolean = false,
    val notificationTime: LocalTime? = null,
    val error: String? = null
)