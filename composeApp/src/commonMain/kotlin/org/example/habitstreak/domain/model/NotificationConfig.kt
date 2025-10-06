package org.example.habitstreak.domain.model

import kotlinx.datetime.LocalTime

/**
 * Simplified notification configuration for a habit
 * Contains only data that should be persisted
 *
 * Sound/vibration preferences are global settings
 * Habit frequency/createdAt are runtime dependencies (passed separately)
 */
data class NotificationConfig(
    val habitId: String,
    val time: LocalTime,
    val isEnabled: Boolean = true,
    val message: String,
    val period: NotificationPeriod = NotificationPeriod.EveryDay
)