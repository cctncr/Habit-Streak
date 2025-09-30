package org.example.habitstreak.presentation.ui.state

import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.model.NotificationError
import org.example.habitstreak.presentation.model.YearMonth
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel

data class HabitDetailUiState(
    val isLoading: Boolean = false,
    val habit: Habit? = null,
    val records: List<HabitRecord> = emptyList(),
    val stats: HabitDetailViewModel.HabitStats = HabitDetailViewModel.HabitStats(),
    val selectedMonth: YearMonth = YearMonth.current(),
    val error: String? = null,
    // Notification related
    val notificationConfig: NotificationConfig? = null,
    val isNotificationEnabled: Boolean = false,
    val notificationTime: LocalTime? = null,
    val notificationError: NotificationError? = null,
    // Notification preferences
    val notificationSoundEnabled: Boolean = true,
    val notificationVibrationEnabled: Boolean = true
)