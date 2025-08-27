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
import org.example.habitstreak.core.util.UuidGenerator
import org.example.habitstreak.data.local.HabitRecord as DataHabitRecord

class HabitRecordRepositoryImpl(
    private val database: HabitDatabase,
    private val dateProvider: DateProvider
) : HabitRecordRepository {

    private val queries = database.habitRecordQueries

    override suspend fun markHabitAsComplete(
        habitId: String,
        date: LocalDate,
        count: Int,
        note: String  // Note parametresi eklendi
    ): Result<HabitRecord> = withContext(Dispatchers.IO) {
        try {
            val existing = queries.selectByHabitAndDate(habitId, date.toString())
                .executeAsOneOrNull()

            val record = if (existing != null) {
                // Mevcut record'u güncelle, note'u koru veya güncelle
                HabitRecord(
                    id = existing.id,
                    habitId = habitId,
                    date = date,
                    completedCount = count,
                    note = note.ifEmpty { existing.note }, // Note'u koru veya güncelle
                    completedAt = dateProvider.today()
                )
            } else {
                // Yeni record oluştur
                HabitRecord(
                    id = UuidGenerator.generateUUID(),
                    habitId = habitId,
                    date = date,
                    completedCount = count,
                    note = note,
                    completedAt = dateProvider.today()
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

    override suspend fun updateRecordNote(
        habitId: String,
        date: LocalDate,
        note: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = queries.selectByHabitAndDate(habitId, date.toString())
                .executeAsOneOrNull()

            if (existing != null) {
                // Mevcut record'u note ile güncelle
                val updatedRecord = DataHabitRecord(
                    id = existing.id,
                    habitId = existing.habitId,
                    date = existing.date,
                    completedCount = existing.completedCount,
                    note = note, // Update note
                    completedAt = existing.completedAt
                )
                queries.transaction {
                    queries.insert(updatedRecord) // INSERT OR REPLACE
                }
                Result.success(Unit)
            } else if (note.isNotBlank()) {
                // Eğer record yoksa ve not boş değilse, yeni record oluştur (0 progress ile)
                val newRecord = DataHabitRecord(
                    id = UuidGenerator.generateUUID(),
                    habitId = habitId,
                    date = date.toString(),
                    completedCount = 0L,
                    note = note,
                    completedAt = dateProvider.today().toString()
                )
                queries.transaction {
                    queries.insert(newRecord)
                }
                Result.success(Unit)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}