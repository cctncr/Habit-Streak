package org.example.habitstreak.presentation.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Android-specific permission launcher for notification permissions
 * Handles Android 13+ POST_NOTIFICATIONS permission and legacy support
 */
@Composable
fun rememberAndroidPermissionLauncher(
    onPermissionResult: (granted: Boolean, canAskAgain: Boolean) -> Unit
): AndroidPermissionLauncher {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val canAskAgain = if (isGranted) {
            true
        } else {
            // Check if we can show rationale (false means "Don't ask again" was selected)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    context as ComponentActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                true // No permission needed for older versions
            }
        }

        onPermissionResult(isGranted, canAskAgain)
    }

    return remember {
        AndroidPermissionLauncher(
            context = context,
            launcher = { permission ->
                permissionLauncher.launch(permission)
            }
        )
    }
}

/**
 * Android permission launcher wrapper
 */
class AndroidPermissionLauncher(
    private val context: Context,
    private val launcher: (String) -> Unit
) {

    /**
     * Request notification permission on Android
     */
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // For older Android versions, notifications are enabled by default
            // But we still need to check if notifications are enabled at the system level
            val areNotificationsEnabled = areNotificationsEnabled()
            // Since we can't request permission on older versions, we simulate a grant
            // The user would need to manually enable notifications in system settings
        }
    }

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For older versions, check if notifications are enabled at system level
            areNotificationsEnabled()
        }
    }

    /**
     * Check if we can request permission (not permanently denied)
     */
    fun canRequestPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = hasNotificationPermission()
            if (hasPermission) {
                true
            } else {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    context as ComponentActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        } else {
            // For older versions, we can always "request" (open settings)
            true
        }
    }

    /**
     * Open app notification settings
     */
    fun openNotificationSettings(): Boolean {
        return try {
            val intent = Intent().apply {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    else -> {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if notifications are enabled at the system level
     * This is useful for pre-Android 13 devices
     */
    private fun areNotificationsEnabled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
                notificationManager.areNotificationsEnabled()
            } else {
                true // Assume enabled for very old versions
            }
        } catch (e: Exception) {
            true // Default to enabled if we can't check
        }
    }
}

/**
 * Extension functions for easy integration
 */
object AndroidPermissionUtils {

    /**
     * Get the permission string for notifications
     */
    fun getNotificationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null // No permission needed for older versions
        }
    }

    /**
     * Check if device supports runtime notification permissions
     */
    fun supportsRuntimeNotificationPermissions(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * Get user-friendly message for permission denial
     */
    fun getPermissionDenialMessage(canAskAgain: Boolean): String {
        return if (canAskAgain) {
            "Notifications help you stay consistent with your habits. You can enable them anytime in settings."
        } else {
            "To enable notifications, please go to Settings > Apps > Habit Streak > Notifications and turn them on."
        }
    }
}