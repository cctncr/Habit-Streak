package org.example.habitstreak.domain.model

/**
 * Enum for habit filtering options following domain-driven design.
 * Moved from presentation layer to domain for better separation of concerns.
 */
enum class HabitFilter {
    ALL,
    COMPLETED,
    PENDING
}