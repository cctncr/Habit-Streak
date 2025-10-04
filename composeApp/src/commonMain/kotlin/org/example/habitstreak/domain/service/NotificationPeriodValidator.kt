package org.example.habitstreak.domain.service

import kotlinx.datetime.DayOfWeek as KotlinxDayOfWeek
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.example.habitstreak.domain.model.DayOfWeek
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.NotificationPeriod
import org.example.habitstreak.domain.util.HabitFrequencyUtils

/**
 * Service for validating and checking notification periods
 * Follows Single Responsibility Principle
 */
class NotificationPeriodValidator {

    /**
     * Check if notification should be sent on specific date
     * based on notification period and habit frequency
     */
    fun shouldNotifyOnDate(
        period: NotificationPeriod,
        date: LocalDate,
        habitFrequency: HabitFrequency,
        habitCreatedAt: LocalDate
    ): Boolean {
        return when (period) {
            is NotificationPeriod.EveryDay -> true

            is NotificationPeriod.ActiveDaysOnly -> {
                HabitFrequencyUtils.isActiveOnDate(
                    frequency = habitFrequency,
                    date = date,
                    habitCreatedAt = habitCreatedAt
                )
            }

            is NotificationPeriod.SelectedDays -> {
                val kotlinxDayOfWeek = date.dayOfWeek
                val dayOfWeek = mapKotlinxDayOfWeekToDayOfWeek(kotlinxDayOfWeek)
                period.daysOfWeek.contains(dayOfWeek)
            }
        }
    }

    /**
     * Get days of week that should receive notifications
     * Returns all 7 days for EveryDay, active days for ActiveDaysOnly,
     * and selected days for SelectedDays
     */
    fun getNotificationDays(
        period: NotificationPeriod,
        habitFrequency: HabitFrequency,
        habitCreatedAt: LocalDate,
        weekStartDate: LocalDate
    ): Set<DayOfWeek> {
        return when (period) {
            is NotificationPeriod.EveryDay -> {
                DayOfWeek.entries.toSet()
            }

            is NotificationPeriod.ActiveDaysOnly -> {
                // Check all 7 days in the week
                (0..6).mapNotNull { offset ->
                    val currentDate = weekStartDate.plus(offset.toLong(), kotlinx.datetime.DateTimeUnit.DAY)
                    val kotlinxDay = currentDate.dayOfWeek
                    if (HabitFrequencyUtils.isActiveOnDate(habitFrequency, currentDate, habitCreatedAt)) {
                        mapKotlinxDayOfWeekToDayOfWeek(kotlinxDay)
                    } else null
                }.toSet()
            }

            is NotificationPeriod.SelectedDays -> {
                period.daysOfWeek
            }
        }
    }

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
}
