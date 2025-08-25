package org.example.habitstreak.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class Habit(
    val id: String = "",
    val title: String,
    val description: String = "",
    val icon: HabitIcon,
    val color: HabitColor,
    val frequency: HabitFrequency,
    val reminderTime: LocalTime? = null,
    val isReminderEnabled: Boolean = false,
    val targetCount: Int = 1, // For habits like "drink 8 glasses of water"
    val unit: String = "", // "glasses", "minutes", "pages" etc.
    val createdAt: LocalDate,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)