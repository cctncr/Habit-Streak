package org.example.habitstreak.core.util

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*

object DateFormatter {
    @Composable
    fun formatMonthShort(date: LocalDate): String {
        return when (date.month.number) {
            1 -> stringResource(Res.string.month_jan)
            2 -> stringResource(Res.string.month_feb)
            3 -> stringResource(Res.string.month_mar)
            4 -> stringResource(Res.string.month_apr)
            5 -> stringResource(Res.string.month_may)
            6 -> stringResource(Res.string.month_jun)
            7 -> stringResource(Res.string.month_jul)
            8 -> stringResource(Res.string.month_aug)
            9 -> stringResource(Res.string.month_sep)
            10 -> stringResource(Res.string.month_oct)
            11 -> stringResource(Res.string.month_nov)
            12 -> stringResource(Res.string.month_dec)
            else -> ""
        }
    }

    @Composable
    fun formatMonthYear(date: LocalDate): String {
        return "${formatMonthShort(date)} ${date.year}"
    }

    fun formatDayOfWeek(day: org.example.habitstreak.domain.model.DayOfWeek): String {
        return day.displayName
    }

    @Composable
    fun formatTime(time: LocalTime): String {
        val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
        val amPm = if (time.hour < 12) stringResource(Res.string.time_am) else stringResource(Res.string.time_pm)
        return "$hour:${time.minute.toString().padStart(2, '0')} $amPm"
    }

    @Composable
    fun formatRelativeDate(date: LocalDate, today: LocalDate): String {
        val daysDiff = date.toEpochDays() - today.toEpochDays()

        return when {
            daysDiff == 0L -> stringResource(Res.string.date_today)
            daysDiff == 1L -> stringResource(Res.string.date_tomorrow)
            daysDiff in 2..6 -> stringResource(Res.string.date_in_days, daysDiff.toInt())
            daysDiff in 7..13 -> stringResource(Res.string.date_next_week)
            daysDiff in 14..30 -> stringResource(Res.string.date_in_weeks, (daysDiff / 7).toInt())
            daysDiff in 31..365 -> stringResource(Res.string.date_in_months, (daysDiff / 30).toInt())
            daysDiff > 365 -> {
                val years = daysDiff / 365
                if (years == 1L) stringResource(Res.string.date_next_year)
                else stringResource(Res.string.date_in_years, years.toInt())
            }
            daysDiff == -1L -> stringResource(Res.string.date_yesterday)
            daysDiff in -6..-2 -> stringResource(Res.string.date_days_ago, (-daysDiff).toInt())
            daysDiff in -13..-7 -> stringResource(Res.string.date_last_week)
            daysDiff in -30..-14 -> stringResource(Res.string.date_weeks_ago, ((-daysDiff) / 7).toInt())
            daysDiff in -365..-31 -> stringResource(Res.string.date_months_ago, ((-daysDiff) / 30).toInt())
            daysDiff < -365 -> {
                val years = -daysDiff / 365
                if (years == 1L) stringResource(Res.string.date_last_year)
                else stringResource(Res.string.date_years_ago, years.toInt())
            }
            else -> formatFullDate(date)
        }
    }

    @Composable
    fun formatFullDate(date: LocalDate): String {
        return "${date.day} ${formatMonthShort(date)} ${date.year}"
    }

    @Composable
    fun formatDateShort(date: LocalDate): String {
        return "${date.day} ${formatMonthShort(date)}"
    }

    @Composable
    fun formatRelativeDateShort(date: LocalDate, today: LocalDate): String {
        val daysDiff = date.toEpochDays() - today.toEpochDays()

        return when {
            daysDiff == 0L -> stringResource(Res.string.date_today)
            daysDiff == 1L -> stringResource(Res.string.date_tomorrow)
            daysDiff == -1L -> stringResource(Res.string.date_yesterday)
            daysDiff in 2..6 -> "+$daysDiff ${stringResource(Res.string.unit_days)}"
            daysDiff in -6..-2 -> "${-daysDiff} ${stringResource(Res.string.date_days_ago)}"
            daysDiff > 6 -> formatDateShort(date)
            daysDiff < -6 -> formatDateShort(date)
            else -> formatDateShort(date)
        }
    }
}