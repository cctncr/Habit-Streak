package org.example.habitstreak.domain.model

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class Habit @OptIn(ExperimentalTime::class) constructor(
    val id: String = "",
    val title: String,
    val description: String,
    val icon: HabitIcon,
    val color: HabitColor,
    val frequency: HabitFrequency,
    val categories: List<Category> = emptyList(),
    val reminderTime: String? = null,
    val isReminderEnabled: Boolean = false,
    val targetCount: Int = 1,
    val unit: String = "",
    val createdAt: Instant = Clock.System.now(),
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)