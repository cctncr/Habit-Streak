package org.example.habitstreak.presentation.ui.state

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel.HabitStats
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class HabitDetailUiState @OptIn(ExperimentalTime::class) constructor(
    val habit: Habit? = null,
    val records: List<HabitRecord> = emptyList(),
    val statistics: HabitStats? = null,
    val filteredStatistics: HabitStats? = null,
    val isLoading: Boolean = true,
    val selectedDate: LocalDate? = null,
    val selectedMonth: HabitDetailViewModel.YearMonth = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).let {
        HabitDetailViewModel.YearMonth(it.year, it.month.number)
    },
    val notificationConfig: NotificationConfig? = null,
    val isNotificationEnabled: Boolean = false,
    val notificationTime: LocalTime? = null,
    val error: String? = null
)