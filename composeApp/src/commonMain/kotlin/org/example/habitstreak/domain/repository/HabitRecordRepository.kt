package org.example.habitstreak.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.model.HabitRecord

interface HabitRecordRepository {
    suspend fun markHabitAsComplete(
        habitId: String,
        date: LocalDate,
        count: Int = 1,
        note: String = ""
    ): Result<HabitRecord>

    suspend fun markHabitAsIncomplete(habitId: String, date: LocalDate): Result<Unit>
    suspend fun getRecordsForHabit(habitId: String): Result<List<HabitRecord>>
    suspend fun getRecordsForDate(date: LocalDate): Result<List<HabitRecord>>
    suspend fun getRecordsBetweenDates(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<HabitRecord>>

    suspend fun updateRecordNote(habitId: String, date: LocalDate, note: String): Result<Unit>
    fun observeRecordsForHabit(habitId: String): Flow<List<HabitRecord>>
    fun observeRecordsForDate(date: LocalDate): Flow<List<HabitRecord>>
}