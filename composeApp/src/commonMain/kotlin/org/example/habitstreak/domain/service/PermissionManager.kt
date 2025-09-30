package org.example.habitstreak.domain.service

import org.example.habitstreak.domain.model.NotificationError

/**
 * Platform-agnostic permission management
 * Following Interface Segregation Principle - focused on permission handling only
 */
interface PermissionManager {

    /**
     * Check if notification permission is granted
     */
    suspend fun hasNotificationPermission(): Boolean

    /**
     * Request notification permission from user
     * @return PermissionResult indicating success/failure and next action
     */
    suspend fun requestNotificationPermission(): PermissionResult

    /**
     * Check if we can show permission request dialog again
     * (false if user selected "Don't ask again")
     */
    suspend fun canRequestPermission(): Boolean

    /**
     * Open app settings for user to manually grant permission
     */
    suspend fun openAppSettings(): Boolean
}

/**
 * Result of permission request
 * Following Single Responsibility Principle
 */
sealed class PermissionResult {

    /** Permission granted successfully */
    data object Granted : PermissionResult()

    /** Permission denied but can ask again */
    data object DeniedCanAskAgain : PermissionResult()

    /** Permission denied permanently - must go to settings */
    data object DeniedPermanently : PermissionResult()

    /** Notifications globally disabled in device/app settings */
    data object GloballyDisabled : PermissionResult()

    /** Error occurred during permission request */
    data class Error(val error: NotificationError) : PermissionResult()
}

/**
 * Extension functions for easy handling
 */
fun PermissionResult.isGranted(): Boolean = this is PermissionResult.Granted

fun PermissionResult.shouldOpenSettings(): Boolean =
    this is PermissionResult.DeniedPermanently || this is PermissionResult.GloballyDisabled

fun PermissionResult.canRetry(): Boolean =
    this is PermissionResult.DeniedCanAskAgain