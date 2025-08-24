package org.example.habit_streak.data

data class Habit(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val colorName: String,
    val frequencyType: String,
    val frequencyData: String, // JSON for complex frequency data
    val reminderTime: String?, // Store as ISO string
    val isReminderEnabled: Boolean,
    val targetCount: Int,
    val unit: String,
    val createdAt: String, // ISO date string
    val isArchived: Boolean,
    val sortOrder: Int
)