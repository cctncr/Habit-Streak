package org.example.habitstreak.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.example.habitstreak.domain.model.NotificationError
import org.example.habitstreak.domain.service.PermissionManager
import org.example.habitstreak.domain.service.PermissionResult
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
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
            notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                val isAuthorized = settings?.authorizationStatus == UNAuthorizationStatusAuthorized ||
                        settings?.authorizationStatus == UNAuthorizationStatusProvisional
                continuation.resume(isAuthorized)
            }
        }
    }

    override suspend fun requestNotificationPermission(): PermissionResult {
        // Check current status first
        val currentStatus = getCurrentAuthorizationStatus()

        return when (currentStatus) {
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
                    NotificationError.GeneralError(Exception("Unknown authorization status"))
                )
            }
        }
    }

    private suspend fun getCurrentAuthorizationStatus(): Long {
        return suspendCancellableCoroutine { continuation ->
            notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                continuation.resume(settings?.authorizationStatus ?: UNAuthorizationStatusNotDetermined)
            }
        }
    }

    private suspend fun requestPermissionFromSystem(): PermissionResult {
        return suspendCancellableCoroutine { continuation ->
            val options = UNAuthorizationOptionAlert or
                    UNAuthorizationOptionBadge or
                    UNAuthorizationOptionSound

            notificationCenter.requestAuthorizationWithOptions(options) { granted, error ->
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
            }
        }
    }

    override suspend fun canRequestPermission(): Boolean {
        val hasRequestedBefore = NSUserDefaults.standardUserDefaults.boolForKey(KEY_PERMISSION_REQUESTED)

        // If we've never requested before, we can request
        if (!hasRequestedBefore) return true

        // If we've requested before, check current status
        val currentStatus = getCurrentAuthorizationStatus()
        return currentStatus == UNAuthorizationStatusNotDetermined
    }

    override suspend fun openAppSettings(): Boolean {
        return try {
            val settingsUrl = UIApplication.openSettingsURLString
            val url = NSURL.URLWithString(settingsUrl)

            if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
                UIApplication.sharedApplication.openURL(url)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}