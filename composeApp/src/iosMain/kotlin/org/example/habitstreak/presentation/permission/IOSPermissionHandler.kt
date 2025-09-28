package org.example.habitstreak.presentation.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.*
import kotlin.coroutines.resume

/**
 * iOS-specific permission handler for notification permissions
 * Handles both provisional and full authorization on iOS
 */
@Composable
fun rememberIOSPermissionHandler(
    onPermissionResult: (granted: Boolean, canAskAgain: Boolean) -> Unit
): IOSPermissionHandler {
    return remember {
        IOSPermissionHandler { granted, canAskAgain ->
            onPermissionResult(granted, canAskAgain)
        }
    }
}

/**
 * iOS permission handler wrapper
 */
class IOSPermissionHandler(
    private val onResult: (granted: Boolean, canAskAgain: Boolean) -> Unit
) {

    /**
     * Request notification permission on iOS
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun requestNotificationPermission(): Pair<Boolean, Boolean> = suspendCancellableCoroutine { continuation ->
        val center = UNUserNotificationCenter.currentNotificationCenter()

        val options = UNAuthorizationOptionAlert or
                     UNAuthorizationOptionSound or
                     UNAuthorizationOptionBadge

        center.requestAuthorizationWithOptions(options) { granted, error ->
            val canAskAgain = error == null // If there's no error, we can potentially ask again
            continuation.resume(Pair(granted, canAskAgain))
        }
    }

    /**
     * Check if notification permission is granted
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun hasNotificationPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        val center = UNUserNotificationCenter.currentNotificationCenter()

        center.getNotificationSettingsWithCompletionHandler { settings ->
            val isAuthorized = when (settings?.authorizationStatus) {
                UNAuthorizationStatusAuthorized,
                UNAuthorizationStatusProvisional -> true
                else -> false
            }
            continuation.resume(isAuthorized)
        }
    }

    /**
     * Check if we can request permission (not permanently denied)
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun canRequestPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        val center = UNUserNotificationCenter.currentNotificationCenter()

        center.getNotificationSettingsWithCompletionHandler { settings ->
            val canRequest = when (settings?.authorizationStatus) {
                UNAuthorizationStatusDenied -> false // User denied, must go to settings
                UNAuthorizationStatusNotDetermined -> true // Can request
                UNAuthorizationStatusAuthorized,
                UNAuthorizationStatusProvisional -> true // Already granted
                else -> true
            }
            continuation.resume(canRequest)
        }
    }

    /**
     * Open app notification settings on iOS
     */
    fun openNotificationSettings(): Boolean {
        return try {
            val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
            settingsUrl?.let { url ->
                val canOpen = UIApplication.sharedApplication.canOpenURL(url)
                if (canOpen) {
                    UIApplication.sharedApplication.openURL(url)
                    true
                } else {
                    false
                }
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check notification authorization status
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun getAuthorizationStatus(): UNAuthorizationStatus = suspendCancellableCoroutine { continuation ->
        val center = UNUserNotificationCenter.currentNotificationCenter()

        center.getNotificationSettingsWithCompletionHandler { settings ->
            continuation.resume(settings?.authorizationStatus ?: UNAuthorizationStatusNotDetermined)
        }
    }

    /**
     * Request provisional authorization (quiet notifications)
     * This allows notifications to be delivered directly to Notification Center
     * without interrupting the user
     */
    @OptIn(ExperimentalForeignApi::class)
    suspend fun requestProvisionalAuthorization(): Boolean = suspendCancellableCoroutine { continuation ->
        val center = UNUserNotificationCenter.currentNotificationCenter()

        val options = UNAuthorizationOptionAlert or
                     UNAuthorizationOptionSound or
                     UNAuthorizationOptionBadge or
                     UNAuthorizationOptionProvisional

        center.requestAuthorizationWithOptions(options) { granted, error ->
            continuation.resume(granted && error == null)
        }
    }

    /**
     * Check if device supports notification features
     */
    fun supportsNotifications(): Boolean {
        return try {
            UNUserNotificationCenter.currentNotificationCenter() != null
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * iOS-specific permission utilities
 */
object IOSPermissionUtils {

    /**
     * Get user-friendly authorization status description
     */
    @OptIn(ExperimentalForeignApi::class)
    fun getStatusDescription(status: UNAuthorizationStatus): String {
        return when (status) {
            UNAuthorizationStatusNotDetermined -> "Not determined - can request permission"
            UNAuthorizationStatusDenied -> "Denied - user must enable in Settings"
            UNAuthorizationStatusAuthorized -> "Authorized - notifications enabled"
            UNAuthorizationStatusProvisional -> "Provisional - quiet notifications enabled"
            else -> "Unknown status"
        }
    }

    /**
     * Check if status allows showing notifications
     */
    @OptIn(ExperimentalForeignApi::class)
    fun canShowNotifications(status: UNAuthorizationStatus): Boolean {
        return status == UNAuthorizationStatusAuthorized ||
               status == UNAuthorizationStatusProvisional
    }

    /**
     * Get appropriate message for permission request based on current status
     */
    @OptIn(ExperimentalForeignApi::class)
    fun getPermissionMessage(status: UNAuthorizationStatus): String {
        return when (status) {
            UNAuthorizationStatusNotDetermined ->
                "Enable notifications to get reminders for your habits and stay on track with your goals."

            UNAuthorizationStatusDenied ->
                "Notifications are currently disabled. To enable them, go to Settings > Notifications > Habit Streak and turn on Allow Notifications."

            UNAuthorizationStatusAuthorized ->
                "Notifications are enabled! You'll receive timely reminders for your habits."

            UNAuthorizationStatusProvisional ->
                "Quiet notifications are enabled. You can upgrade to full notifications in Settings for more timely reminders."

            else ->
                "Notifications help you maintain consistent habits. Enable them in Settings for the best experience."
        }
    }
}