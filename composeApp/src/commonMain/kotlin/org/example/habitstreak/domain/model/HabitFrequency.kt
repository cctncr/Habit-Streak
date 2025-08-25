package org.example.habitstreak.domain.model

sealed class HabitFrequency {
    data object Daily : HabitFrequency()
    data class Weekly(val daysOfWeek: Set<DayOfWeek>) : HabitFrequency()
    data class Monthly(val daysOfMonth: Set<Int>) : HabitFrequency()
    data class Custom(val repeatInterval: Int, val repeatUnit: RepeatUnit) : HabitFrequency()
}