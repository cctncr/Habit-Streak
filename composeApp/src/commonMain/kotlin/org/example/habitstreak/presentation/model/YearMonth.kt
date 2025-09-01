package org.example.habitstreak.presentation.model

import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class YearMonth(
    val year: Int,
    val month: Int
) {
    init {
        require(month in 1..12) { "Month must be between 1 and 12, was $month" }
    }

    companion object {
        @OptIn(ExperimentalTime::class)
        fun now(): YearMonth {
            val now = Clock.System.now()
            val localDateTime = now.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            return YearMonth(localDateTime.year, localDateTime.monthNumber)
        }
    }
}