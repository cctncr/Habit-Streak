package org.example.habit_streak.domain.model

import kotlinx.datetime.LocalDate

data class HabitStatistics(
    val habitId: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionRate: Float, // 0.0 to 1.0
    val totalCompletions: Int,
    val lastCompletedDate: LocalDate?
)