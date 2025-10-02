package org.example.habitstreak.domain.usecase.notification

import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.NotificationError
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalTime

/**
 * Use case to enable global notifications and re-enable all habit notifications
 * Following Single Responsibility Principle - only handles global enablement
 */
class EnableGlobalNotificationsUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val notificationService: NotificationService,
    private val habitRepository: HabitRepository
) {

    /**
     * Enable global notifications and re-enable all habit notifications
     */
    suspend fun execute(): NotificationOperationResult {
        println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Starting global notification enablement")

        return try {
            // Step 1: Enable global notifications setting
            preferencesRepository.setNotificationsEnabled(true)
            println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Global notifications preference set to true")

            // Step 2: Re-enable all habit notifications that were previously enabled
            println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Re-enabling all habit notifications")
            val habitResult = enableAllHabitNotifications()

            println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Global enablement completed")
            habitResult

        } catch (e: Exception) {
            println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Error occurred: ${e.message}")
            NotificationOperationResult.error(e, "Failed to enable global notifications")
        }
    }

    /**
     * Re-enable all habit notifications that have reminders configured
     * Returns result with success/partial success/failure information
     */
    private suspend fun enableAllHabitNotifications(): NotificationOperationResult {
        // Get all active habits with reminders
        val habits = try {
            habitRepository.observeActiveHabits().first()
                .filter { it.isReminderEnabled && !it.reminderTime.isNullOrEmpty() }
        } catch (e: Exception) {
            println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Error fetching habits: ${e.message}")
            return NotificationOperationResult.error(e, "Failed to fetch habits")
        }

        println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Found ${habits.size} habits with reminders")

        // If no habits with reminders, success
        if (habits.isEmpty()) {
            return NotificationOperationResult.Success
        }

        val successes = mutableListOf<String>()
        val failures = mutableListOf<HabitOperationFailure>()

        // Process each habit and track results
        habits.forEach { habit ->
            try {
                val time = LocalTime.parse(habit.reminderTime!!)
                println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Enabling notification for habit: ${habit.title} at $time")

                notificationService.enableNotification(habit.id, time).fold(
                    onSuccess = {
                        successes.add(habit.id)
                        println("✓ Successfully enabled: ${habit.title}")
                    },
                    onFailure = { error ->
                        failures.add(createFailure(habit, error))
                        println("✗ Failed to enable: ${habit.title} - ${error.message}")
                    }
                )
            } catch (e: Exception) {
                failures.add(createFailure(habit, e))
                println("✗ Exception enabling: ${habit.title} - ${e.message}")
            }
        }

        // Return appropriate result based on success/failure counts
        return when {
            failures.isEmpty() -> {
                println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: All ${successes.size} habits enabled successfully")
                NotificationOperationResult.Success
            }
            successes.isEmpty() -> {
                println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: All ${failures.size} habits failed to enable")
                NotificationOperationResult.Error(
                    NotificationError.GeneralError(
                        cause = Exception("All habits failed to enable"),
                        message = "Failed to enable notifications for all habits"
                    )
                )
            }
            else -> {
                println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Partial success - ${successes.size} succeeded, ${failures.size} failed")
                NotificationOperationResult.PartialSuccess(
                    successCount = successes.size,
                    failureCount = failures.size,
                    failures = failures
                )
            }
        }
    }

    /**
     * Create a failure record from a habit and error
     */
    private fun createFailure(habit: Habit, error: Throwable): HabitOperationFailure {
        return when (error) {
            is IllegalArgumentException -> HabitOperationFailure(
                habitId = habit.id,
                habitTitle = habit.title,
                errorType = FailureType.INVALID_TIME_FORMAT,
                errorMessage = "Invalid time format: ${habit.reminderTime}",
                canRetry = false
            )
            is NotificationError -> when (error) {
                is NotificationError.PermissionDenied -> HabitOperationFailure(
                    habitId = habit.id,
                    habitTitle = habit.title,
                    errorType = FailureType.PERMISSION_DENIED,
                    errorMessage = error.message,
                    canRetry = error.canRequestAgain
                )
                is NotificationError.ServiceUnavailable -> HabitOperationFailure(
                    habitId = habit.id,
                    habitTitle = habit.title,
                    errorType = FailureType.SERVICE_UNAVAILABLE,
                    errorMessage = error.message,
                    canRetry = false
                )
                is NotificationError.SchedulingFailed -> HabitOperationFailure(
                    habitId = habit.id,
                    habitTitle = habit.title,
                    errorType = FailureType.SCHEDULING_ERROR,
                    errorMessage = error.message,
                    canRetry = true
                )
                else -> HabitOperationFailure(
                    habitId = habit.id,
                    habitTitle = habit.title,
                    errorType = FailureType.UNKNOWN,
                    errorMessage = error.message ?: "Unknown error",
                    canRetry = true
                )
            }
            else -> HabitOperationFailure(
                habitId = habit.id,
                habitTitle = habit.title,
                errorType = FailureType.UNKNOWN,
                errorMessage = error.message ?: "Unknown error",
                canRetry = true
            )
        }
    }
}