package org.example.habit_streak.presentation.ui.state

import kotlinx.datetime.LocalTime
import org.example.habit_streak.domain.model.HabitColor
import org.example.habit_streak.domain.model.HabitFrequency
import org.example.habit_streak.domain.model.HabitIcon

data class CreateEditHabitUiState(
    val title: String = "",
    val description: String = "",
    val selectedIcon: HabitIcon = HabitIcon.STAR,
    val selectedColor: HabitColor = HabitColor.SKY,
    val frequency: HabitFrequency = HabitFrequency.Daily,
    val reminderTime: LocalTime? = null,
    val targetCount: Int = 1,
    val unit: String = "",
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)