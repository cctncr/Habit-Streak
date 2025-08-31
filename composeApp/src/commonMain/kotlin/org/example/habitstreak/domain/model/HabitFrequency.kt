package org.example.habitstreak.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class HabitFrequency {
    @Serializable
    object Daily : HabitFrequency()

    @Serializable
    data class Weekly(val daysOfWeek: Set<DayOfWeek>) : HabitFrequency()

    @Serializable
    data class Monthly(val daysOfMonth: Set<Int>) : HabitFrequency()

    @Serializable
    data class Custom(
        val repeatInterval: Int,
        val repeatUnit: RepeatUnit
    ) : HabitFrequency()
}