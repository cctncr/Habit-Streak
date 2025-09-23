package org.example.habitstreak.domain.service

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.habitstreak.domain.model.HabitFilter
import org.example.habitstreak.domain.usecase.habit.GetHabitsWithCompletionUseCase
import org.example.habitstreak.domain.util.HabitFrequencyUtils
import kotlin.time.ExperimentalTime

/**
 * Service for habit filtering logic following Single Responsibility Principle.
 * Extracted from UI layer to maintain clean separation of concerns.
 */
@OptIn(ExperimentalTime::class)
class HabitFilterService {

    /**
     * Filters habits based on specified criteria.
     * Pure business logic without UI concerns.
     */
    fun filterHabits(
        habits: List<GetHabitsWithCompletionUseCase.HabitWithCompletion>,
        filter: HabitFilter,
        selectedCategoryId: String?,
        targetDate: LocalDate
    ): List<GetHabitsWithCompletionUseCase.HabitWithCompletion> {
        return habits.filter { habitWithCompletion ->
            val matchesFilter = when (filter) {
                HabitFilter.ALL -> true
                HabitFilter.COMPLETED -> isHabitCompleted(habitWithCompletion, targetDate)
                HabitFilter.PENDING -> isHabitPending(habitWithCompletion, targetDate)
            }

            val matchesCategory = selectedCategoryId == null ||
                habitWithCompletion.habit.categories.any { it.id == selectedCategoryId }

            matchesFilter && matchesCategory
        }
    }

    /**
     * Checks if a habit is active on a given date based on its frequency settings.
     */
    fun isHabitActiveOnDate(
        habitWithCompletion: GetHabitsWithCompletionUseCase.HabitWithCompletion,
        date: LocalDate
    ): Boolean {
        val habitCreatedAt = habitWithCompletion.habit.createdAt.toLocalDateTime(
            TimeZone.currentSystemDefault()
        ).date

        return HabitFrequencyUtils.isActiveOnDate(
            habitWithCompletion.habit.frequency,
            date,
            habitCreatedAt
        )
    }

    /**
     * Determines if a habit is completed for the target date.
     */
    private fun isHabitCompleted(
        habitWithCompletion: GetHabitsWithCompletionUseCase.HabitWithCompletion,
        targetDate: LocalDate
    ): Boolean {
        return isHabitActiveOnDate(habitWithCompletion, targetDate) &&
               habitWithCompletion.completedCount >= habitWithCompletion.habit.targetCount
    }

    /**
     * Determines if a habit is pending (active but not completed) for the target date.
     */
    private fun isHabitPending(
        habitWithCompletion: GetHabitsWithCompletionUseCase.HabitWithCompletion,
        targetDate: LocalDate
    ): Boolean {
        return isHabitActiveOnDate(habitWithCompletion, targetDate) &&
               habitWithCompletion.completedCount < habitWithCompletion.habit.targetCount
    }

    /**
     * Calculates progress statistics for a list of habits.
     */
    fun calculateProgressStats(
        habits: List<GetHabitsWithCompletionUseCase.HabitWithCompletion>,
        targetDate: LocalDate
    ): ProgressStats {
        val activeHabits = habits.filter { isHabitActiveOnDate(it, targetDate) }
        val completedHabits = activeHabits.filter {
            it.completedCount >= it.habit.targetCount
        }

        return ProgressStats(
            totalHabits = activeHabits.size,
            completedHabits = completedHabits.size,
            completionRate = if (activeHabits.isNotEmpty()) {
                completedHabits.size.toFloat() / activeHabits.size.toFloat()
            } else 0f
        )
    }

    data class ProgressStats(
        val totalHabits: Int,
        val completedHabits: Int,
        val completionRate: Float
    )
}