package org.example.habitstreak.presentation.ui.state

import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.Category
import org.example.habitstreak.domain.model.HabitColor
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.HabitIcon

data class CreateEditHabitUiState(
    val title: String = "",
    val description: String = "",
    val selectedIcon: HabitIcon = HabitIcon.STAR,
    val selectedColor: HabitColor = HabitColor.SKY,
    val frequency: HabitFrequency = HabitFrequency.Daily,
    val selectedCategories: List<Category> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val reminderTime: LocalTime? = null,
    val targetCount: Int = 1,
    val unit: String = "",
    val isEditMode: Boolean = false,
    val isArchived: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCustomCategoryDialog: Boolean = false,
    val customCategoryName: String = ""
)