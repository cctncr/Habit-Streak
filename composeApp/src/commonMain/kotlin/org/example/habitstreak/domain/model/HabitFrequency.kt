package org.example.habitstreak.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class HabitFrequency {
    @Serializable
    object Daily : HabitFrequency()

    @Serializable
    data class Weekly(val days: Set<DayOfWeek>) : HabitFrequency()

    @Serializable
    data class Custom(val daysOfMonth: Set<Int>) : HabitFrequency()

    @Serializable
    data class Monthly(
        val dayOfMonth: Int,
        val repeatUnit: String = "months"
    ) : HabitFrequency()
}