package org.example.habitstreak.domain.usecase.notification

import org.example.habitstreak.domain.model.NotificationError

/**
 * Unified result type for all notification operations
 * Following SOLID principles and consistent error handling
 */
sealed class NotificationOperationResult {
    /**
     * Operation completed successfully
     */
    data object Success : NotificationOperationResult()

    /**
     * Operation failed with a typed error
     */
    data class Error(val error: NotificationError) : NotificationOperationResult()

    /**
     * Operation requires notification permission
     */
    data object PermissionRequired : NotificationOperationResult()

    /**
     * Batch operation partially succeeded (some habits succeeded, others failed)
     */
    data class PartialSuccess(
        val successCount: Int,
        val failureCount: Int,
        val failures: List<HabitOperationFailure>
    ) : NotificationOperationResult()

    companion object {
        /**
         * Create Error from string message
         */
        fun error(message: String): Error = Error(
            NotificationError.GeneralError(
                cause = Exception(message),
                message = message
            )
        )

        /**
         * Create Error from throwable
         */
        fun error(throwable: Throwable, message: String? = null): Error = Error(
            NotificationError.GeneralError(
                cause = throwable,
                message = message ?: throwable.message ?: "An error occurred"
            )
        )
    }
}

/**
 * Represents a single habit operation failure in batch operations
 */
data class HabitOperationFailure(
    val habitId: String,
    val habitTitle: String,
    val errorType: FailureType,
    val errorMessage: String,
    val canRetry: Boolean
)

/**
 * Types of failures that can occur during habit notification operations
 */
enum class FailureType {
    INVALID_TIME_FORMAT,
    SCHEDULING_ERROR,
    SERVICE_UNAVAILABLE,
    PERMISSION_DENIED,
    DATABASE_ERROR,
    UNKNOWN
}
