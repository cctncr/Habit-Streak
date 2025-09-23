package org.example.habitstreak.domain.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.DayOfWeek as KotlinxDayOfWeek
import org.example.habitstreak.domain.model.DayOfWeek
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.RepeatUnit

/**
 * Utility class for handling habit frequency logic
 * Follows SOLID principles with single responsibility for frequency calculations
 */
object HabitFrequencyUtils {

    /**
     * Checks if a habit should be tracked on a specific date based on its frequency
     * This now works for all dates, including before creation date for proper striped patterns
     */
    fun isActiveOnDate(frequency: HabitFrequency, date: LocalDate, habitCreatedAt: LocalDate): Boolean {
        return when (frequency) {
            is HabitFrequency.Daily -> true

            is HabitFrequency.Weekly -> {
                val kotlinxDayOfWeek = date.dayOfWeek
                val dayOfWeek = mapKotlinxDayOfWeekToDayOfWeek(kotlinxDayOfWeek)
                frequency.daysOfWeek.contains(dayOfWeek)
            }

            is HabitFrequency.Monthly -> {
                frequency.daysOfMonth.contains(date.day)
            }

            is HabitFrequency.Custom -> {
                when (frequency.repeatUnit) {
                    RepeatUnit.DAYS -> {
                        // For days before creation, calculate backwards from creation date
                        val daysDifference = (date.toEpochDays() - habitCreatedAt.toEpochDays()).toInt()
                        daysDifference % frequency.repeatInterval == 0
                    }
                    RepeatUnit.WEEKS -> {
                        // For weeks, calculate based on week difference from creation week
                        val daysDifference = (date.toEpochDays() - habitCreatedAt.toEpochDays()).toInt()
                        val weeksDifference = daysDifference / 7
                        weeksDifference % frequency.repeatInterval == 0
                    }
                    RepeatUnit.MONTHS -> {
                        // For months, check if it's the same day of month in the right interval
                        val monthsDifference = (date.year - habitCreatedAt.year) * 12 +
                                                (date.monthNumber - habitCreatedAt.monthNumber)
                        monthsDifference % frequency.repeatInterval == 0 && date.day == habitCreatedAt.day
                    }
                }
            }
        }
    }

    /**
     * Maps kotlinx.datetime DayOfWeek to our custom DayOfWeek enum
     */
    private fun mapKotlinxDayOfWeekToDayOfWeek(kotlinxDayOfWeek: KotlinxDayOfWeek): DayOfWeek {
        return when (kotlinxDayOfWeek) {
            KotlinxDayOfWeek.MONDAY -> DayOfWeek.MONDAY
            KotlinxDayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
            KotlinxDayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
            KotlinxDayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
            KotlinxDayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
            KotlinxDayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
            KotlinxDayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
    }

    /**
     * Gets the frequency description for display purposes
     */
    fun getFrequencyDescription(frequency: HabitFrequency): String {
        return when (frequency) {
            is HabitFrequency.Daily -> "Daily"

            is HabitFrequency.Weekly -> {
                val dayNames = frequency.daysOfWeek.map { it.displayName }
                when (dayNames.size) {
                    1 -> "Weekly on ${dayNames.first()}"
                    7 -> "Daily"
                    else -> "Weekly on ${dayNames.joinToString(", ")}"
                }
            }

            is HabitFrequency.Monthly -> {
                val dayNumbers = frequency.daysOfMonth.sorted()
                "Monthly on ${dayNumbers.joinToString(", ")}"
            }

            is HabitFrequency.Custom -> {
                when (frequency.repeatUnit) {
                    RepeatUnit.DAYS -> "Every ${frequency.repeatInterval} days"
                    RepeatUnit.WEEKS -> "Every ${frequency.repeatInterval} weeks"
                    RepeatUnit.MONTHS -> "Every ${frequency.repeatInterval} months"
                }
            }
        }
    }
}