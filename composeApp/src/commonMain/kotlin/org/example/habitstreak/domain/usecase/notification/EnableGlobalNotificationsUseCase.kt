package org.example.habitstreak.domain.usecase.notification

import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.repository.HabitRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalTime

/**
 * Use case to enable global notifications and re-enable all habit notifications
 * Following Single Responsibility Principle - only handles global enablement
 */
class EnableGlobalNotificationsUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val notificationService: NotificationService?,
    private val habitRepository: HabitRepository
) {

    sealed class GlobalEnableResult {
        data object Success : GlobalEnableResult()
        data class Error(val message: String) : GlobalEnableResult()
    }

    /**
     * Enable global notifications and re-enable all habit notifications
     */
    suspend fun execute(): GlobalEnableResult {
        println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Starting global notification enablement")

        return try {
            // Step 1: Enable global notifications setting
            preferencesRepository.setNotificationsEnabled(true)
            println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Global notifications preference set to true")

            // Step 2: Re-enable all habit notifications that were previously enabled
            if (notificationService != null) {
                println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Re-enabling all habit notifications")
                enableAllHabitNotifications()
            } else {
                println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: NotificationService is null, skipping habit notifications")
            }

            println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Global enablement completed successfully")
            GlobalEnableResult.Success

        } catch (e: Exception) {
            println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Error occurred: ${e.message}")
            GlobalEnableResult.Error("Failed to enable global notifications: ${e.message}")
        }
    }

    /**
     * Re-enable all habit notifications that have reminders configured
     */
    private suspend fun enableAllHabitNotifications() {
        if (notificationService == null) return

        try {
            // Get all active habits with reminders
            val habits = habitRepository.observeActiveHabits().first()
            println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Found ${habits.size} active habits")

            habits.forEach { habit ->
                if (habit.isReminderEnabled && !habit.reminderTime.isNullOrEmpty()) {
                    try {
                        val time = LocalTime.parse(habit.reminderTime)
                        println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Re-enabling notification for habit: ${habit.title} at $time")

                        notificationService.enableNotification(
                            habitId = habit.id,
                            time = time
                        )
                    } catch (e: Exception) {
                        // Log error but continue with other habits
                        println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Failed to enable notification for habit ${habit.title}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            println("🔔 ENABLE_GLOBAL_NOTIFICATIONS_USECASE: Error enabling habit notifications: ${e.message}")
            // Don't throw - global setting was successful
        }
    }
}