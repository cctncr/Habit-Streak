package org.example.habitstreak.core.extensions

import androidx.compose.runtime.Composable
import kotlinx.datetime.*
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.domain.util.DateFormatter
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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
 * Extension function to check if a date is in current week
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

fun LocalDate.isInCurrentWeekFromMonday(today: LocalDate): Boolean {
    val startOfWeek = today.startOfWeekFromMonday()
    val endOfWeek = startOfWeek.plus(DatePeriod(days = 6))
    return this in startOfWeek..endOfWeek
}

fun LocalDate.startOfWeekFromMonday(): LocalDate {
    val mondayBasedDayOfWeek = when (this.dayOfWeek) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
    }
    return this.minus(DatePeriod(days = mondayBasedDayOfWeek - 1))
}

@Composable
fun formatShort(date: LocalDate): String {
    return DateFormatter.formatFullDate(date)
}

@Composable
fun formatLong(date: LocalDate): String {
    val dayOfWeek = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$dayOfWeek, ${DateFormatter.formatFullDate(date)}"
}

@OptIn(ExperimentalTime::class)
@Composable
fun formatRelative(date: LocalDate): String {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return DateFormatter.formatRelativeDate(date, today)
}