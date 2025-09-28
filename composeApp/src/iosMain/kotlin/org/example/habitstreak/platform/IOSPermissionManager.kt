package org.example.habitstreak.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.example.habitstreak.domain.model.NotificationError
import org.example.habitstreak.domain.service.PermissionManager
import org.example.habitstreak.domain.service.PermissionResult
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS-specific permission manager
 * Following Single Responsibility Principle
 */
@OptIn(ExperimentalForeignApi::class)
class IOSPermissionManager : PermissionManager {

    companion object {
        private const val KEY_PERMISSION_REQUESTED = "notification_permission_requested"
    }

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun hasNotificationPermission(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            try {
                notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                    try {
                        val isAuthorized = settings?.authorizationStatus == UNAuthorizationStatusAuthorized ||
                                settings?.authorizationStatus == UNAuthorizationStatusProvisional
                        continuation.resume(isAuthorized)
                    } catch (e: Exception) {
                        // If we can't determine permission status, assume false for safety
                        continuation.resume(false)
                    }
                }
            } catch (e: Exception) {
                // If notification center fails, assume no permission
                continuation.resume(false)
            }
        }
    }

    override suspend fun requestNotificationPermission(): PermissionResult {
        return try {
            // Check current status first
            val currentStatus = getCurrentAuthorizationStatus()

            when (currentStatus) {
                UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional -> {
                    PermissionResult.Granted
                }
                UNAuthorizationStatusDenied -> {
                    PermissionResult.DeniedPermanently
                }
                UNAuthorizationStatusNotDetermined -> {
                    // Request permission
                    requestPermissionFromSystem()
                }
                else -> {
                    PermissionResult.Error(
                        NotificationError.GeneralError(Exception("Unknown authorization status: $currentStatus"))
                    )
                }
            }
        } catch (e: Exception) {
            PermissionResult.Error(
                NotificationError.GeneralError(Exception("Failed to request notification permission: ${e.message}", e))
            )
        }
    }

    private suspend fun getCurrentAuthorizationStatus(): Long {
        return suspendCancellableCoroutine { continuation ->
            try {
                notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                    try {
                        continuation.resume(settings?.authorizationStatus ?: UNAuthorizationStatusNotDetermined)
                    } catch (e: Exception) {
                        // If we can't determine status, assume not determined
                        continuation.resume(UNAuthorizationStatusNotDetermined)
                    }
                }
            } catch (e: Exception) {
                // If notification center fails, assume not determined
                continuation.resume(UNAuthorizationStatusNotDetermined)
            }
        }
    }

    private suspend fun requestPermissionFromSystem(): PermissionResult {
        return suspendCancellableCoroutine { continuation ->
            try {
                val options = UNAuthorizationOptionAlert or
                        UNAuthorizationOptionBadge or
                        UNAuthorizationOptionSound

                notificationCenter.requestAuthorizationWithOptions(options) { granted, error ->
                    try {
                        // Mark that we've requested permission
                        NSUserDefaults.standardUserDefaults.setBool(true, KEY_PERMISSION_REQUESTED)

                        when {
                            error != null -> {
                                continuation.resume(
                                    PermissionResult.Error(
                                        NotificationError.GeneralError(Exception("Permission request error: ${error.localizedDescription}"))
                                    )
                                )
                            }
                            granted -> {
                                continuation.resume(PermissionResult.Granted)
                            }
                            else -> {
                                // Permission denied
                                continuation.resume(PermissionResult.DeniedPermanently)
                            }
                        }
                    } catch (e: Exception) {
                        continuation.resume(
                            PermissionResult.Error(
                                NotificationError.GeneralError(Exception("Failed to process permission response: ${e.message}", e))
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                continuation.resume(
                    PermissionResult.Error(
                        NotificationError.GeneralError(Exception("Failed to request permission from system: ${e.message}", e))
                    )
                )
            }
        }
    }

    override suspend fun canRequestPermission(): Boolean {
        return try {
            val hasRequestedBefore = NSUserDefaults.standardUserDefaults.boolForKey(KEY_PERMISSION_REQUESTED)

            // If we've never requested before, we can request
            if (!hasRequestedBefore) return true

            // If we've requested before, check current status
            val currentStatus = getCurrentAuthorizationStatus()
            currentStatus == UNAuthorizationStatusNotDetermined
        } catch (e: Exception) {
            // If we can't determine if we can request, assume we can't
            false
        }
    }

    override suspend fun openAppSettings(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            try {
                val settingsUrl = UIApplicationOpenSettingsURLString
                val url = NSURL.URLWithString(settingsUrl)

                if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
                    // Use the non-deprecated openURL:options:completionHandler: method
                    UIApplication.sharedApplication.openURL(
                        url,
                        options = emptyMap<Any?, Any>()
                    ) { success ->
                        continuation.resume(success)
                    }
                } else {
                    continuation.resume(false)
                }
            } catch (e: Exception) {
                continuation.resume(false)
            }
        }
    }
}