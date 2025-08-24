package org.example.habit_streak.data

data class HabitRecord(
    val id: String,
    val habitId: String,
    val date: String, // ISO date string
    val completedCount: Int,
    val note: String,
    val completedAt: String // ISO date string
)