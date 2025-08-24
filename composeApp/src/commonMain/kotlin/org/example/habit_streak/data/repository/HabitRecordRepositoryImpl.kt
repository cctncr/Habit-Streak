package org.example.habit_streak.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.habit_streak.data.local.HabitDatabase
import org.example.habit_streak.data.mapper.toData
import org.example.habit_streak.data.mapper.toDomain
import org.example.habit_streak.domain.model.HabitRecord
import org.example.habit_streak.domain.repository.HabitRecordRepository
import java.util.UUID

class HabitRecordRepositoryImpl(
    private val database: HabitDatabase
) : HabitRecordRepository {

    private val queries = database.habitRecordQueries

    override suspend fun markHabitAsComplete(
        habitId: String,
        date: LocalDate,
        count: Int
    ): Result<HabitRecord> {
        return try {
            val existing = queries.selectByHabitAndDate(habitId, date.toString())
                .executeAsOneOrNull()

            val record = if (existing != null) {
                existing.toDomain().copy(completedCount = count)
            } else {
                HabitRecord(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    date = date,
                    completedCount = count,
                    note = "",
                    completedAt = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                )
            }

            queries.insert(record.toData())
            Result.success(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markHabitAsIncomplete(habitId: String, date: LocalDate): Result<Unit> {
        return try {
            queries.deleteByHabitAndDate(habitId, date.toString())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecordsForHabit(habitId: String): Result<List<HabitRecord>> {
        return try {
            val records = queries.selectByHabit(habitId)
                .executeAsList()
                .map { it.toDomain() }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecordsForDate(date: LocalDate): Result<List<HabitRecord>> {
        return try {
            val records = queries.selectByDate(date.toString())
                .executeAsList()
                .map { it.toDomain() }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecordsBetweenDates(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<HabitRecord>> {
        return try {
            val records = queries.selectBetweenDates(
                startDate.toString(),
                endDate.toString()
            ).executeAsList().map { it.toDomain() }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeRecordsForHabit(habitId: String): Flow<List<HabitRecord>> {
        return queries.selectByHabit(habitId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun observeRecordsForDate(date: LocalDate): Flow<List<HabitRecord>> {
        return queries.selectByDate(date.toString())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }
}