package org.example.habitstreak.domain.model

/**
 * Sealed class for notification errors - Multi-language safe!
 * Following Single Responsibility Principle
 */
sealed class NotificationError : Exception() {

    /**
     * Permission denied - need to request permission or go to settings
     */
    data class PermissionDenied(
        val canRequestAgain: Boolean = true,
        override val message: String = "Notification permission denied"
    ) : NotificationError()

    /**
     * Global notifications disabled in app settings
     */
    data class GloballyDisabled(
        override val message: String = "Notifications are globally disabled"
    ) : NotificationError()

    /**
     * Service unavailable (no NotificationService)
     */
    data class ServiceUnavailable(
        override val message: String = "Notification service not available"
    ) : NotificationError()

    /**
     * Habit not found
     */
    data class HabitNotFound(
        val habitId: String,
        override val message: String = "Habit not found"
    ) : NotificationError()

    /**
     * Scheduling failed (platform-specific error)
     */
    data class SchedulingFailed(
        val reason: String,
        override val message: String = "Failed to schedule notification"
    ) : NotificationError()

    /**
     * General error
     */
    data class GeneralError(
        override val cause: Throwable,
        override val message: String = "An error occurred"
    ) : NotificationError()

    // Helper method for checking if error should be preserved
    fun shouldPreserve(): Boolean = when (this) {
        is PermissionDenied -> true
        is GloballyDisabled -> true
        is ServiceUnavailable -> true
        else -> false
    }

    // Helper method for checking if error requires user action
    fun requiresUserAction(): Boolean = when (this) {
        is PermissionDenied -> true
        is GloballyDisabled -> true
        else -> false
    }
}