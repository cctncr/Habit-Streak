package org.example.habitstreak.presentation.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import org.example.habitstreak.domain.service.PermissionResult
import org.example.habitstreak.platform.IOSPermissionManager
import org.koin.compose.koinInject
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional

/**
 * iOS implementation of platform permission launcher
 */
@OptIn(ExperimentalForeignApi::class)
class IOSPlatformPermissionLauncher(
    private val permissionManager: IOSPermissionManager,
    private val onResult: (granted: Boolean, canAskAgain: Boolean) -> Unit
) : PlatformPermissionLauncher {

    override fun requestNotificationPermission() {
        // Since iOS permission request is async, we can't launch it directly from here
        // The actual request should be handled through the permission manager
        // This will be called from UI and we'll use coroutines to handle the async nature
    }

    override suspend fun hasNotificationPermission(): Boolean {
        return permissionManager.hasNotificationPermission()
    }

    override suspend fun canRequestPermission(): Boolean {
        return permissionManager.canRequestPermission()
    }

    /**
     * Request permission asynchronously and handle result
     */
    suspend fun requestPermissionAsync() {
        val result = permissionManager.requestNotificationPermission()

        when (result) {
            is PermissionResult.Granted -> {
                onResult(true, true)
            }
            is PermissionResult.DeniedCanAskAgain -> {
                onResult(false, true)
            }
            is PermissionResult.DeniedPermanently -> {
                onResult(false, false)
            }
            is PermissionResult.GloballyDisabled -> {
                onResult(false, false)
            }
            is PermissionResult.Error -> {
                onResult(false, false)
            }
        }
    }
}

@Composable
actual fun rememberPlatformPermissionLauncher(
    onPermissionResult: (granted: Boolean, canAskAgain: Boolean) -> Unit
): PlatformPermissionLauncher {
    val permissionManager = koinInject<IOSPermissionManager>()
    val coroutineScope = rememberCoroutineScope()

    return remember(permissionManager) {
        object : PlatformPermissionLauncher {
            override fun requestNotificationPermission() {
                coroutineScope.launch {
                    val launcher = IOSPlatformPermissionLauncher(permissionManager, onPermissionResult)
                    launcher.requestPermissionAsync()
                }
            }

            override suspend fun hasNotificationPermission(): Boolean {
                return permissionManager.hasNotificationPermission()
            }

            override suspend fun canRequestPermission(): Boolean {
                return permissionManager.canRequestPermission()
            }
        }
    }
}