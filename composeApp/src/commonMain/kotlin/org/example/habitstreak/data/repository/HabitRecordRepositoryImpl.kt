package org.example.habitstreak.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.example.habitstreak.data.local.HabitDatabase
import org.example.habitstreak.data.mapper.toData
import org.example.habitstreak.data.mapper.toDomain
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.domain.util.UuidGenerator
import kotlin.time.ExperimentalTime

class HabitRecordRepositoryImpl(
    database: HabitDatabase,
    private val dateProvider: DateProvider
) : HabitRecordRepository {

    private val queries = database.habitRecordQueries

    @OptIn(ExperimentalTime::class)
    override suspend fun markHabitAsComplete(
        habitId: String,
        date: LocalDate,
        count: Int,
        note: String
    ): Result<HabitRecord> = withContext(Dispatchers.IO) {
        try {
            val existing = queries.selectByHabitAndDate(habitId, date.toString())
                .executeAsOneOrNull()

            val record = if (existing != null) {
                HabitRecord(
                    id = existing.id,
                    habitId = habitId,
                    date = date,
                    completedCount = count,
                    note = note.ifEmpty { existing.note },
                    completedAt = dateProvider.now()
                )
            } else {
                HabitRecord(
                    id = UuidGenerator.generateUUID(),
                    habitId = habitId,
                    date = date,
                    completedCount = count,
                    note = note,
                    completedAt = dateProvider.now()
                )
            }

            queries.transaction {
                queries.insert(record.toData())
            }

            Result.success(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markHabitAsIncomplete(
        habitId: String,
        date: LocalDate
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.transaction {
                queries.deleteByHabitAndDate(habitId, date.toString())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecordsForHabit(habitId: String): Result<List<HabitRecord>> =
        withContext(Dispatchers.IO) {
            try {
                val records = queries.selectByHabit(habitId)
                    .executeAsList()
                    .map { it.toDomain() }
                Result.success(records)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getRecordsForDate(date: LocalDate): Result<List<HabitRecord>> =
        withContext(Dispatchers.IO) {
            try {
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
    ): Result<List<HabitRecord>> = withContext(Dispatchers.IO) {
        try {
            val records = queries.selectBetweenDates(startDate.toString(), endDate.toString())
                .executeAsList()
                .map { it.toDomain() }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRecordNote(habitId: String, date: LocalDate, note: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val existing = queries.selectByHabitAndDate(habitId, date.toString())
                    .executeAsOneOrNull()

                if (existing != null) {
                    val updated = existing.copy(note = note)
                    queries.insert(updated)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun observeAllRecords(): Flow<List<HabitRecord>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
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