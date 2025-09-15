package org.example.habitstreak.core.error

/**
 * Exception thrown when notification permission is denied by the user
 */
class NotificationPermissionDeniedException : Exception("Notification permission denied")

/**
 * Exception thrown when a habit with the specified ID is not found
 */
class HabitNotFoundException(habitId: String) : Exception("Habit not found: $habitId")

/**
 * Exception thrown when notification configuration is not found for a habit
 */
class NotificationNotFoundException(habitId: String) :
    Exception("Notification config not found for habit: $habitId")

/**
 * Exception thrown when notifications are globally disabled in settings
 */
class NotificationsDisabledException : Exception("Notifications are globally disabled")

/**
 * Exception thrown when default categories cannot be deleted
 */
class DefaultCategoryDeleteException : Exception("Default categories cannot be deleted")