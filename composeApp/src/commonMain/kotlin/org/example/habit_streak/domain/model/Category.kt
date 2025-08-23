package org.example.habit_streak.domain.model

data class Category(
    val id: String = "",
    val name: String,
    val color: HabitColor,
    val icon: HabitIcon,
    val sortOrder: Int = 0
)