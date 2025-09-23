package org.example.habitstreak.domain.service

import org.example.habitstreak.domain.model.HabitFrequency

/**
 * Service for habit validation logic following Single Responsibility Principle.
 * Extracted from Use Cases to maintain clean separation of concerns.
 */
class HabitValidationService {

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<ValidationError> = emptyList()
    )

    sealed class ValidationError(val message: String) {
        object EmptyTitle : ValidationError("Title cannot be empty")
        object TitleTooLong : ValidationError("Title cannot exceed 50 characters")
        object DescriptionTooLong : ValidationError("Description cannot exceed 200 characters")
        object InvalidTargetCount : ValidationError("Target count must be greater than 0")
        object EmptyUnit : ValidationError("Unit cannot be empty when target count is specified")
        object InvalidFrequency : ValidationError("Invalid frequency configuration")
    }

    fun validateHabitCreation(
        title: String,
        description: String,
        targetCount: Int,
        unit: String,
        frequency: HabitFrequency
    ): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Title validation
        if (title.isBlank()) {
            errors.add(ValidationError.EmptyTitle)
        } else if (title.length > 50) {
            errors.add(ValidationError.TitleTooLong)
        }

        // Description validation
        if (description.length > 200) {
            errors.add(ValidationError.DescriptionTooLong)
        }

        // Target count validation
        if (targetCount <= 0) {
            errors.add(ValidationError.InvalidTargetCount)
        }

        // Unit validation
        if (targetCount > 1 && unit.isBlank()) {
            errors.add(ValidationError.EmptyUnit)
        }

        // Frequency validation
        if (!isValidFrequency(frequency)) {
            errors.add(ValidationError.InvalidFrequency)
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    private fun isValidFrequency(frequency: HabitFrequency): Boolean {
        return when (frequency) {
            is HabitFrequency.Daily -> true
            is HabitFrequency.Weekly -> frequency.daysOfWeek.isNotEmpty()
            is HabitFrequency.Monthly -> frequency.daysOfMonth.isNotEmpty() &&
                frequency.daysOfMonth.all { it in 1..31 }
            is HabitFrequency.Custom -> frequency.repeatInterval > 0
        }
    }
}