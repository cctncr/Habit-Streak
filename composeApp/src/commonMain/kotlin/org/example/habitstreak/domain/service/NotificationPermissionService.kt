package org.example.habitstreak.domain.service

import org.example.habitstreak.domain.model.NotificationError

/**
 * Dedicated service for notification permission management
 * Following Single Responsibility Principle - only handles permissions
 */
class NotificationPermissionService(
    private val permissionManager: PermissionManager
) {

    /**
     * Check current permission status
     */
    suspend fun checkPermissionStatus(): PermissionResult {
        println("🔔 NOTIFICATION_PERMISSION_SERVICE: checkPermissionStatus called")
        return try {
            val hasPermission = permissionManager.hasNotificationPermission()
            println("🔔 NOTIFICATION_PERMISSION_SERVICE: hasNotificationPermission = $hasPermission")

            if (hasPermission) {
                println("🔔 NOTIFICATION_PERMISSION_SERVICE: Permission granted, returning Granted")
                PermissionResult.Granted
            } else {
                // First try to get the precise status from the permission manager
                val requestResult = permissionManager.requestNotificationPermission()
                println("🔔 NOTIFICATION_PERMISSION_SERVICE: requestNotificationPermission returned $requestResult")
                requestResult
            }
        } catch (e: Exception) {
            println("🔔 NOTIFICATION_PERMISSION_SERVICE: Exception occurred: ${e.message}")
            PermissionResult.Error(NotificationError.GeneralError(e))
        }
    }

    /**
     * Request notification permission
     */
    suspend fun requestPermission(): PermissionResult {
        return permissionManager.requestNotificationPermission()
    }

    /**
     * Check if permission is currently granted
     */
    suspend fun hasPermission(): Boolean {
        return try {
            permissionManager.hasNotificationPermission()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if we can request permission (not permanently denied)
     */
    suspend fun canRequestPermission(): Boolean {
        return try {
            permissionManager.canRequestPermission()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Open app settings for permission management
     */
    suspend fun openAppSettings(): Boolean {
        return try {
            permissionManager.openAppSettings()
        } catch (e: Exception) {
            false
        }
    }
}