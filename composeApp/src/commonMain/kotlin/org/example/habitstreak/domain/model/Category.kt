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
        // Predefined categories
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
    }
}