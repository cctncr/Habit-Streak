package org.example.habit_streak.domain.usecase

import kotlinx.datetime.LocalDate
import org.example.habit_streak.domain.repository.HabitRecordRepository
import org.example.habit_streak.domain.usecase.util.UseCase

class ToggleHabitCompletionUseCase(
    private val habitRecordRepository: HabitRecordRepository
) : UseCase<ToggleHabitCompletionUseCase.Params, Result<Unit>> {

    data class Params(
        val habitId: String,
        val date: LocalDate,
        val count: Int = 1
    )

    override suspend fun invoke(params: Params): Result<Unit> {
        val existingRecords = habitRecordRepository.getRecordsForDate(params.date)

        return existingRecords.fold(
            onSuccess = { records ->
                val existingRecord = records.find { it.habitId == params.habitId }
                if (existingRecord != null) {
                    habitRecordRepository.markHabitAsIncomplete(params.habitId, params.date)
                } else {
                    habitRecordRepository.markHabitAsComplete(params.habitId, params.date, params.count)
                        .map { }
                }
            },
            onFailure = { Result.failure(it) }
        )
    }
}