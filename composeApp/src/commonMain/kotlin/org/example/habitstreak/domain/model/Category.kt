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
            "Beslenme",
            "Finans",
            "Fitness",
            "Sanat",
            "Sağlık",
            "Sosyal",
            "Çalışma",
            "İş",
            "Sabah",
            "Akşam",
            "Diğer"
        )

        // Category colors for visual distinction
        val CATEGORY_COLORS = mapOf(
            "Beslenme" to HabitColor.PEACH,
            "Finans" to HabitColor.GOLD,
            "Fitness" to HabitColor.CORAL,
            "Sanat" to HabitColor.LAVENDER,
            "Sağlık" to HabitColor.MINT,
            "Sosyal" to HabitColor.SKY,
            "Çalışma" to HabitColor.PERIWINKLE,
            "İş" to HabitColor.SLATE,
            "Sabah" to HabitColor.SUNSET,
            "Akşam" to HabitColor.OCEAN,
            "Diğer" to HabitColor.CHARCOAL
        )

        // Category icons for visual distinction
        val CATEGORY_ICONS = mapOf(
            "Beslenme" to HabitIcon.FOOD,
            "Finans" to HabitIcon.MONEY,
            "Fitness" to HabitIcon.EXERCISE,
            "Sanat" to HabitIcon.ART,
            "Sağlık" to HabitIcon.HEART,
            "Sosyal" to HabitIcon.CHAT,
            "Çalışma" to HabitIcon.STUDY,
            "İş" to HabitIcon.WORK,
            "Sabah" to HabitIcon.COFFEE,
            "Akşam" to HabitIcon.SLEEP,
            "Diğer" to HabitIcon.STAR
        )
    }
}