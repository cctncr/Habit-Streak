package org.example.habitstreak.domain.model

import kotlinx.datetime.LocalTime

data class NotificationConfig(
    val habitId: String,
    val time: LocalTime,
    val isEnabled: Boolean = true,
    val message: String = "Time to complete your habit!"
)