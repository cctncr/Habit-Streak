package org.example.habitstreak.presentation.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.example.habitstreak.platform.DesktopPermissionManager
import org.koin.compose.koinInject

/**
 * Desktop implementation of platform permission launcher
 * Desktop platforms typically don't require runtime permission requests for notifications
 */
class DesktopPlatformPermissionLauncher(
    private val permissionManager: DesktopPermissionManager
) : PlatformPermissionLauncher {

    override fun requestNotificationPermission() {
        // Desktop platforms typically don't require permission requests
        // Notifications are usually handled at the system level
    }

    override suspend fun hasNotificationPermission(): Boolean {
        return permissionManager.hasNotificationPermission()
    }

    override suspend fun canRequestPermission(): Boolean {
        return permissionManager.canRequestPermission()
    }
}

@Composable
actual fun rememberPlatformPermissionLauncher(
    onPermissionResult: (granted: Boolean, canAskAgain: Boolean) -> Unit
): PlatformPermissionLauncher {
    val permissionManager = koinInject<DesktopPermissionManager>()

    return remember(permissionManager) {
        DesktopPlatformPermissionLauncher(permissionManager)
    }
}