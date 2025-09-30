package org.example.habitstreak.presentation.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.example.habitstreak.domain.service.PermissionManager
import org.koin.compose.koinInject

/**
 * Android implementation of platform permission launcher
 */
class AndroidPlatformPermissionLauncher(
    private val context: Context,
    private val launcher: (String) -> Unit,
    private val permissionManager: PermissionManager
) : PlatformPermissionLauncher {

    override fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // For older Android versions, notifications are granted by default
            // but we need to check system settings
            // Since we can't request permission, we'll simulate a grant
            // The user would need to manually enable notifications in system settings
        }
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
    val context = LocalContext.current
    val permissionManager = koinInject<PermissionManager>()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val canAskAgain = if (isGranted) {
            true
        } else {
            // Check if we can show rationale (false means "Don't ask again" was selected)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        context as ComponentActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } catch (e: Exception) {
                    true // Default to true if we can't check
                }
            } else {
                true // No permission needed for older versions
            }
        }

        // Note: Permission state is now managed through the service layer

        onPermissionResult(isGranted, canAskAgain)
    }

    return remember {
        AndroidPlatformPermissionLauncher(
            context = context,
            launcher = { permission ->
                permissionLauncher.launch(permission)
            },
            permissionManager = permissionManager
        )
    }
}