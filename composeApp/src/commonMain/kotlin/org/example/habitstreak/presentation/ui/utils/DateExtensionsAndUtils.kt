package org.example.habitstreak.presentation.ui.utils

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.example.habitstreak.domain.util.DateProvider

/**
 * Date formatting utilities for UI
 */
object DateFormatter {
    fun formatMonthShort(date: LocalDate): String {
        return when(date.month.number) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> ""
        }
    }

    fun formatMonthYear(date: LocalDate): String {
        return "${formatMonthShort(date)} ${date.year}"
    }

    fun formatDayOfWeek(day: org.example.habitstreak.domain.model.DayOfWeek): String {
        return day.displayName
    }

    fun formatTime(time: LocalTime): String {
        val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
        val amPm = if (time.hour < 12) "AM" else "PM"
        return "$hour:${time.minute.toString().padStart(2, '0')} $amPm"
    }

    fun formatRelativeDate(date: LocalDate, today: LocalDate): String {
        val daysDiff = today.toEpochDays() - date.toEpochDays()
        return when (daysDiff) {
            0L -> "Today"
            1L -> "Yesterday"
            in 2..6 -> "$daysDiff days ago"
            in 7..13 -> "Last week"
            in 14..30 -> "${daysDiff / 7} weeks ago"
            in 31..365 -> "${daysDiff / 30} months ago"
            else -> "More than a year ago"
        }
    }
}

/**
 * Extension functions for DateProvider
 */
fun DateProvider.todayMillis(): Long {
    return today().toEpochDays() * 24 * 60 * 60 * 1000L
}

fun DateProvider.isToday(date: LocalDate): Boolean {
    return date == today()
}

fun DateProvider.isYesterday(date: LocalDate): Boolean {
    return date == today().minus(DatePeriod(days = 1))
}

fun DateProvider.daysAgo(days: Int): LocalDate {
    return today().minus(DatePeriod(days = days))
}

fun DateProvider.daysFromNow(days: Int): LocalDate {
    return today().plus(DatePeriod(days = days))
}

/**
 * Extension functions for LocalDate
 */
fun LocalDate.isInCurrentWeek(today: LocalDate): Boolean {
    val startOfWeek = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))
    val endOfWeek = startOfWeek.plus(DatePeriod(days = 6))
    return this in startOfWeek..endOfWeek
}

fun LocalDate.isInCurrentMonth(today: LocalDate): Boolean {
    return this.year == today.year && this.monthNumber == today.monthNumber
}

fun LocalDate.startOfWeek(): LocalDate {
    return this.minus(DatePeriod(days = this.dayOfWeek.ordinal))
}

fun LocalDate.endOfWeek(): LocalDate {
    return this.plus(DatePeriod(days = 6 - this.dayOfWeek.ordinal))
}

fun LocalDate.startOfMonth(): LocalDate {
    return LocalDate(this.year, this.monthNumber, 1)
}

fun LocalDate.endOfMonth(): LocalDate {
    val nextMonth = if (this.monthNumber == 12) {
        LocalDate(this.year + 1, 1, 1)
    } else {
        LocalDate(this.year, this.monthNumber + 1, 1)
    }
    return nextMonth.minus(DatePeriod(days = 1))
}

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