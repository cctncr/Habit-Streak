package org.example.habitstreak.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class HabitRecord @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    val habitId: String,
    val date: LocalDate,
    val completedCount: Int = 1,
    val note: String = "",
    val completedAt: Instant
)