package org.example.habitstreak.presentation.model

import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Simple model for representing Year and Month
 */
data class YearMonth(
    val year: Int,
    val month: Int // 1-12
) {
    init {
        require(month in 1..12) { "Month must be between 1 and 12" }
    }

    companion object {
        @OptIn(ExperimentalTime::class)
        fun current(): YearMonth {
            val now = Clock.System.now()
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            return YearMonth(now.year, now.month.number)
        }

        fun from(date: kotlinx.datetime.LocalDate): YearMonth {
            return YearMonth(date.year, date.month.number)
        }
    }

    fun next(): YearMonth {
        return if (month == 12) {
            YearMonth(year + 1, 1)
        } else {
            YearMonth(year, month + 1)
        }
    }

    fun previous(): YearMonth {
        return if (month == 1) {
            YearMonth(year - 1, 12)
        } else {
            YearMonth(year, month - 1)
        }
    }
}