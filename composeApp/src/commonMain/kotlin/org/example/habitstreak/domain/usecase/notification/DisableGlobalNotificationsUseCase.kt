package org.example.habitstreak.domain.usecase.notification

import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.repository.HabitRepository
import kotlinx.coroutines.flow.first

/**
 * Use case to disable global notifications and disable all habit notifications
 * Symmetric to EnableGlobalNotificationsUseCase
 * Following Single Responsibility Principle
 */
class DisableGlobalNotificationsUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val notificationService: NotificationService?,
    private val habitRepository: HabitRepository
) {

    sealed class GlobalDisableResult {
        data object Success : GlobalDisableResult()
        data class Error(val message: String) : GlobalDisableResult()
    }

    /**
     * Disable global notifications and disable all habit notifications
     */
    suspend fun execute(): GlobalDisableResult {
        println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Starting global notification disablement")

        return try {
            // Step 1: Disable global notifications setting
            preferencesRepository.setNotificationsEnabled(false)
            println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Global notifications preference set to false")

            // Step 2: Disable all habit notifications
            if (notificationService != null) {
                println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Disabling all habit notifications")
                disableAllHabitNotifications()
            } else {
                println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: NotificationService is null, skipping habit notifications")
            }

            println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Global disablement completed successfully")
            GlobalDisableResult.Success

        } catch (e: Exception) {
            println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Error occurred: ${e.message}")
            GlobalDisableResult.Error("Failed to disable global notifications: ${e.message}")
        }
    }

    /**
     * Disable all habit notifications
     */
    private suspend fun disableAllHabitNotifications() {
        if (notificationService == null) return

        try {
            // Get all active habits
            val habits = habitRepository.observeActiveHabits().first()
            println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Found ${habits.size} active habits")

            habits.forEach { habit ->
                try {
                    println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Disabling notification for habit: ${habit.title}")
                    notificationService.disableNotification(habit.id)
                } catch (e: Exception) {
                    // Log error but continue with other habits
                    println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Failed to disable notification for habit ${habit.title}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Error disabling habit notifications: ${e.message}")
            // Don't throw - global setting was successful
        }
    }
}