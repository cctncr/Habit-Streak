package org.example.habitstreak.presentation.ui.utils

import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object DateFormatter {

    fun formatShort(date: LocalDate): String {
        return "${date.day} ${getMonthShort(date.month.number)} ${date.year}"
    }

    fun formatLong(date: LocalDate): String {
        return "${getDayOfWeekName(date.dayOfWeek)}, ${date.day} ${getMonthName(date.month.number)} ${date.year}"
    }

    @OptIn(ExperimentalTime::class)
    fun formatRelative(date: LocalDate): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val daysDiff = (today.toEpochDays() - date.toEpochDays()).toInt()

        return when (daysDiff) {
            0 -> "Today"
            1 -> "Yesterday"
            -1 -> "Tomorrow"
            in 2..7 -> "$daysDiff days ago"
            in -7..-2 -> "In ${-daysDiff} days"
            else -> formatShort(date)
        }
    }

    fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d %s", displayHour, minute, period)
    }

    private fun getMonthName(month: Int): String = when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> ""
    }

    private fun getMonthShort(month: Int): String = when (month) {
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

    private fun getDayOfWeekName(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
        DayOfWeek.MONDAY -> "Monday"
        DayOfWeek.TUESDAY -> "Tuesday"
        DayOfWeek.WEDNESDAY -> "Wednesday"
        DayOfWeek.THURSDAY -> "Thursday"
        DayOfWeek.FRIDAY -> "Friday"
        DayOfWeek.SATURDAY -> "Saturday"
        DayOfWeek.SUNDAY -> "Sunday"
    }
}