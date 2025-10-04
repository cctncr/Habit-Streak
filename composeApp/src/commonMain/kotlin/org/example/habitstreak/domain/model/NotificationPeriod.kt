package org.example.habitstreak.domain.model

import kotlinx.serialization.Serializable

/**
 * Notification reminder period types
 * Defines when notifications should be sent for a habit
 */
@Serializable
sealed class NotificationPeriod {
    /**
     * Notification every day
     */
    @Serializable
    data object EveryDay : NotificationPeriod()

    /**
     * Notification only on active days based on habit frequency
     */
    @Serializable
    data object ActiveDaysOnly : NotificationPeriod()

    /**
     * Notification on specific selected days of week
     */
    @Serializable
    data class SelectedDays(val daysOfWeek: Set<DayOfWeek>) : NotificationPeriod()
}
