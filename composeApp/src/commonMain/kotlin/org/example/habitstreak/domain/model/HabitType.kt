package org.example.habitstreak.domain.model

/**
 * Type of habit tracking
 */
enum class HabitType {
    YES_NO,      // Simple checkbox - done or not done
    COUNTABLE    // Countable target (e.g., 3 glasses of water, 50 pushups)
}

// Extension function to determine type from Habit
fun Habit.getType(): HabitType {
    return if (targetCount > 1) HabitType.COUNTABLE else HabitType.YES_NO
}