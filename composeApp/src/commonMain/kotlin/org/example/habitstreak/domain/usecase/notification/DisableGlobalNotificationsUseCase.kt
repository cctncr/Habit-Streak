package org.example.habitstreak.domain.usecase.notification

import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.NotificationService

/**
 * Use case to disable global notifications and cancel all habit notifications
 * Uses bulk cancel operation for better performance
 * Following Single Responsibility Principle
 */
class DisableGlobalNotificationsUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val notificationService: NotificationService
) {

    /**
     * Disable global notifications and disable all habit notifications
     */
    suspend fun execute(): NotificationOperationResult {
        println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Starting global notification disablement")

        return try {
            // Step 1: Disable global notifications setting
            preferencesRepository.setNotificationsEnabled(false)
            println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Global notifications preference set to false")

            // Step 2: Disable all habit notifications
            println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Disabling all habit notifications")
            disableAllHabitNotifications()

            println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Global disablement completed successfully")
            NotificationOperationResult.Success

        } catch (e: Exception) {
            println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Error occurred: ${e.message}")
            NotificationOperationResult.error(e, "Failed to disable global notifications")
        }
    }

    /**
     * Disable all habit notifications using bulk cancel operation
     * Much more efficient than canceling one by one (O(1) vs O(n))
     */
    private suspend fun disableAllHabitNotifications() {
        try {
            // Use cancelAllNotifications for bulk operation (much faster!)
            // This will cancel all scheduled notifications and disable them in database
            println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Canceling all notifications with bulk operation")

            notificationService.cancelAllNotifications().fold(
                onSuccess = {
                    println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: All notifications canceled successfully")
                },
                onFailure = { error ->
                    println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Error canceling all notifications: ${error.message}")
                    // Don't throw - global setting was successful
                }
            )
        } catch (e: Exception) {
            println("🔔 DISABLE_GLOBAL_NOTIFICATIONS_USECASE: Error disabling habit notifications: ${e.message}")
            // Don't throw - global setting was successful
        }
    }
}