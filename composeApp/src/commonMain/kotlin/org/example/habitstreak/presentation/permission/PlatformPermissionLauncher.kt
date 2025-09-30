package org.example.habitstreak.presentation.permission

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic permission launcher interface
 * This provides a unified API across all platforms
 */
interface PlatformPermissionLauncher {
    /**
     * Launch platform-specific permission request
     */
    fun requestNotificationPermission()

    /**
     * Check if permission is currently granted
     */
    suspend fun hasNotificationPermission(): Boolean

    /**
     * Check if we can request permission (not permanently denied)
     */
    suspend fun canRequestPermission(): Boolean
}

/**
 * Platform-agnostic permission launcher composable
 * Returns the appropriate launcher based on current platform
 */
@Composable
expect fun rememberPlatformPermissionLauncher(
    onPermissionResult: (granted: Boolean, canAskAgain: Boolean) -> Unit
): PlatformPermissionLauncher

/**
 * Common permission result handler
 */
class PermissionResultHandler(
    private val onResult: (granted: Boolean, canAskAgain: Boolean) -> Unit
) {
    fun handleResult(granted: Boolean, canAskAgain: Boolean) {
        onResult(granted, canAskAgain)
    }
}