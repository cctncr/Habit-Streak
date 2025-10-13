package org.example.habitstreak.presentation.ui.components.card

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.example.habitstreak.domain.model.HabitRecord

data class GridDateRange(
    val effectiveStartDate: LocalDate,
    val actualHistoryDays: Long
)

object GridDateHelper {

    fun calculateDateRange(
        today: LocalDate,
        habitRecords: List<HabitRecord>,
        minHistoryDays: Int,
        alignToMonday: Boolean = false
    ): GridDateRange {
        val oldestRecordDate = habitRecords.minOfOrNull { it.date }
        val minRawStartDate = today.minus(DatePeriod(days = minHistoryDays))

        val minStartDate = if (alignToMonday) {
            minRawStartDate.minus(DatePeriod(days = minRawStartDate.dayOfWeek.ordinal))
        } else {
            minRawStartDate
        }

        val effectiveStartDate = if (oldestRecordDate != null && oldestRecordDate < minStartDate) {
            if (alignToMonday) {
                oldestRecordDate.minus(DatePeriod(days = oldestRecordDate.dayOfWeek.ordinal))
            } else {
                oldestRecordDate
            }
        } else {
            minStartDate
        }

        val actualHistoryDays = today.toEpochDays() - effectiveStartDate.toEpochDays() + 1

        return GridDateRange(
            effectiveStartDate = effectiveStartDate,
            actualHistoryDays = actualHistoryDays
        )
    }
}
