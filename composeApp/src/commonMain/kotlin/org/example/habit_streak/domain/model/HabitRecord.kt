package org.example.habit_streak.domain.model

import kotlinx.datetime.LocalDate

data class HabitRecord(
    val id: String = "",
    val habitId: String,
    val date: LocalDate,
    val completedCount: Int = 1,
    val note: String = "",
    val completedAt: LocalDate
)