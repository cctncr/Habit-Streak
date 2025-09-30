package org.example.habitstreak.domain.usecase.notification

import kotlinx.coroutines.flow.first
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.PermissionManager

/**
 * Use case to check if global notifications can be enabled for a habit
 * Following Single Responsibility Principle - only checks status
 */
class CheckGlobalNotificationStatusUseCase(
    private val permissionManager: PermissionManager,
    private val preferencesRepository: PreferencesRepository
) {

    sealed class GlobalNotificationStatus {
        data object CanEnable : GlobalNotificationStatus()
        data object NeedsSystemPermission : GlobalNotificationStatus()
        data object NeedsGlobalEnable : GlobalNotificationStatus()
        data object AlreadyEnabled : GlobalNotificationStatus()
    }

    /**
     * Check the current global notification status
     * @return status indicating what needs to be done to enable notifications
     */
    suspend fun execute(): GlobalNotificationStatus {
        println("🔔 CHECK_GLOBAL_NOTIFICATION_STATUS_USECASE: Checking global notification status...")

        // Step 1: Check if system permission is granted
        val hasSystemPermission = permissionManager.hasNotificationPermission()
        println("🔔 CHECK_GLOBAL_NOTIFICATION_STATUS_USECASE: Has system permission: $hasSystemPermission")

        if (!hasSystemPermission) {
            println("🔔 CHECK_GLOBAL_NOTIFICATION_STATUS_USECASE: System permission not granted, returning NeedsSystemPermission")
            return GlobalNotificationStatus.NeedsSystemPermission
        }

        // Step 2: Check if global notifications are enabled
        val globalNotificationsEnabled = preferencesRepository.isNotificationsEnabled().first()
        println("🔔 CHECK_GLOBAL_NOTIFICATION_STATUS_USECASE: Global notifications enabled: $globalNotificationsEnabled")

        return if (globalNotificationsEnabled) {
            println("🔔 CHECK_GLOBAL_NOTIFICATION_STATUS_USECASE: All conditions met, returning AlreadyEnabled")
            GlobalNotificationStatus.AlreadyEnabled
        } else {
            println("🔔 CHECK_GLOBAL_NOTIFICATION_STATUS_USECASE: System permission granted but global disabled, returning NeedsGlobalEnable")
            GlobalNotificationStatus.NeedsGlobalEnable
        }
    }
}