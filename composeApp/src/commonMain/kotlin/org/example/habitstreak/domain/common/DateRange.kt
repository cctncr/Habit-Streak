package org.example.habitstreak.domain.common

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.example.habitstreak.domain.util.DateProvider

/**
 * Date range utilities
 */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    fun contains(date: LocalDate): Boolean = date in start..end

    fun days(): List<LocalDate> {
        val days = mutableListOf<LocalDate>()
        var current = start
        while (current <= end) {
            days.add(current)
            current = current.plus(DatePeriod(days = 1))
        }
        return days
    }

    val numberOfDays: Int
        get() = (end.toEpochDays() - start.toEpochDays() + 1).toInt()
}

fun DateProvider.lastWeek(): DateRange {
    val end = today()
    val start = end.minus(DatePeriod(days = 6))
    return DateRange(start, end)
}

fun DateProvider.lastMonth(): DateRange {
    val end = today()
    val start = end.minus(DatePeriod(days = 29))
    return DateRange(start, end)
}

fun DateProvider.lastYear(): DateRange {
    val end = today()
    val start = end.minus(DatePeriod(days = 364))
    return DateRange(start, end)
}