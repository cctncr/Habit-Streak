package org.example.habitstreak.domain.model

import kotlinx.datetime.LocalTime
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.Serializable

@Serializable
data class NotificationConfig(
    val habitId: String,
    val time: LocalTime,
    val isEnabled: Boolean = true,
    val message: String? = null,
    val repeatDays: Set<DayOfWeek> = DayOfWeek.entries.toSet()
)