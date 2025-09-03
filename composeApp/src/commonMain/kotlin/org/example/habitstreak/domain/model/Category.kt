package org.example.habitstreak.domain.model

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class Category @OptIn(ExperimentalTime::class) constructor(
    val id: String = "",
    val name: String,
    val isCustom: Boolean = false,
    val usageCount: Int = 0,
    val createdAt: Instant = Clock.System.now()
) {
    companion object {
        // Predefined categories in English
        val PREDEFINED_CATEGORIES = listOf(
            "Nutrition",
            "Finance",
            "Fitness",
            "Art",
            "Health",
            "Social",
            "Work",
            "Business",
            "Morning",
            "Evening",
            "Afternoon",
            "Other"
        )

        // Category colors for visual distinction
        val CATEGORY_COLORS = mapOf(
            "Nutrition" to HabitColor.PEACH,
            "Finance" to HabitColor.GOLD,
            "Fitness" to HabitColor.CORAL,
            "Art" to HabitColor.LAVENDER,
            "Health" to HabitColor.MINT,
            "Social" to HabitColor.SKY,
            "Work" to HabitColor.PERIWINKLE,
            "Business" to HabitColor.SLATE,
            "Morning" to HabitColor.SUNSET,
            "Evening" to HabitColor.OCEAN,
            "Afternoon" to HabitColor.SAND,
            "Other" to HabitColor.CHARCOAL
        )

        // Category icons for visual distinction
        val CATEGORY_ICONS = mapOf(
            "Nutrition" to HabitIcon.FOOD,
            "Finance" to HabitIcon.MONEY,
            "Fitness" to HabitIcon.EXERCISE,
            "Art" to HabitIcon.ART,
            "Health" to HabitIcon.HEART,
            "Social" to HabitIcon.CHAT,
            "Work" to HabitIcon.STUDY,
            "Business" to HabitIcon.WORK,
            "Morning" to HabitIcon.COFFEE,
            "Evening" to HabitIcon.SLEEP,
            "Afternoon" to HabitIcon.TARGET,
            "Other" to HabitIcon.STAR
        )
    }
}