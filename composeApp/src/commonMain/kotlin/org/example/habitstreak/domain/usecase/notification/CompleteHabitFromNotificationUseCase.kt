package org.example.habitstreak.domain.usecase.notification

import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.usecase.UseCase

/**
 * Use case for completing habits from notification actions
 */
class CompleteHabitFromNotificationUseCase(
    private val habitRecordRepository: HabitRecordRepository
) : UseCase<CompleteHabitFromNotificationUseCase.Params, Result<Unit>> {

    data class Params(
        val habitId: String,
        val date: LocalDate,
        val count: Int = 1,
        val note: String = "Completed from notification"
    )

    override suspend fun invoke(params: Params): Result<Unit> {
        return try {
            // Check if habit is already completed for the date
            val existingRecords = habitRecordRepository.getRecordsForDate(params.date)
                .getOrNull() ?: emptyList()

            val existingRecord = existingRecords.find { it.habitId == params.habitId }

            if (existingRecord != null) {
                // Habit already completed, return success without duplicating
                Result.success(Unit)
            } else {
                // Mark habit as complete
                habitRecordRepository.markHabitAsComplete(
                    habitId = params.habitId,
                    date = params.date,
                    count = params.count,
                    note = params.note
                ).map { Unit }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}