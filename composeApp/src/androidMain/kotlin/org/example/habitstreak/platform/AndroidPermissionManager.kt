package org.example.habitstreak.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.example.habitstreak.domain.model.NotificationError
import org.example.habitstreak.domain.service.PermissionManager
import org.example.habitstreak.domain.service.PermissionResult
import androidx.core.content.edit

class AndroidPermissionManager(
    private val context: Context,
    private val activityProvider: ActivityProvider
) : PermissionManager {

    companion object {
        private const val PREFS_KEY = "permission_prefs"
        private const val KEY_PERMISSION_DENIED_PERMANENTLY = "notification_denied_permanently"
    }

    override suspend fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            // Even if permission is granted, check if notifications are enabled at system level
            val notificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            val hasFullPermission = permission && notificationsEnabled

            println("🔔 ANDROID_PERMISSION_MANAGER: hasNotificationPermission (API 33+) permission=$permission, notificationsEnabled=$notificationsEnabled, combined=$hasFullPermission")
            hasFullPermission
        } else {
            // For API < 33, notification permission is granted by default
            val notificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            println("🔔 ANDROID_PERMISSION_MANAGER: hasNotificationPermission (API < 33) areNotificationsEnabled = $notificationsEnabled")
            notificationsEnabled
        }
    }

    override suspend fun requestNotificationPermission(): PermissionResult {
        println("🔔 ANDROID_PERMISSION_MANAGER: requestNotificationPermission called")

        // If already granted, return success
        if (hasNotificationPermission()) {
            println("🔔 ANDROID_PERMISSION_MANAGER: Permission already granted, returning Granted")
            return PermissionResult.Granted
        }

        // For API < 33, check if notifications are enabled in system settings
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val notificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            println("🔔 ANDROID_PERMISSION_MANAGER: API < 33, areNotificationsEnabled = $notificationsEnabled")
            return if (notificationsEnabled) {
                println("🔔 ANDROID_PERMISSION_MANAGER: API < 33, notifications enabled, returning Granted")
                PermissionResult.Granted
            } else {
                println("🔔 ANDROID_PERMISSION_MANAGER: API < 33, notifications disabled in system settings, returning GloballyDisabled")
                // Notifications disabled in system settings - must go to settings
                PermissionResult.GloballyDisabled
            }
        }

        if (!canRequestPermissionSync()) {
            println("🔔 ANDROID_PERMISSION_MANAGER: Cannot request permission, returning DeniedPermanently")
            return PermissionResult.DeniedPermanently
        }

        // For API 33+, also check if user has permission but notifications are disabled at system level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            val notificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()

            if (permission && !notificationsEnabled) {
                println("🔔 ANDROID_PERMISSION_MANAGER: API 33+, permission granted but notifications disabled at system level, returning GloballyDisabled")
                return PermissionResult.GloballyDisabled
            }
        }

        // NOTE: This method is called after the actual permission request has been made
        // by the PlatformPermissionLauncher. It should determine the result based on current state.
        val activity = activityProvider.getCurrentActivity()
        if (activity == null) {
            return PermissionResult.Error(
                NotificationError.ServiceUnavailable("Activity context required for permission request")
            )
        }

        return try {
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )

            // Check if permission was just granted
            if (hasNotificationPermission()) {
                PermissionResult.Granted
            } else if (!shouldShowRationale && hasBeenRequestedBefore()) {
                // User denied and selected "Don't ask again"
                PermissionResult.DeniedPermanently
            } else {
                // User denied but can ask again
                markPermissionRequested()
                PermissionResult.DeniedCanAskAgain
            }
        } catch (e: Exception) {
            PermissionResult.Error(NotificationError.GeneralError(e))
        }
    }

    override suspend fun canRequestPermission(): Boolean {
        return canRequestPermissionSync()
    }

    private suspend fun canRequestPermissionSync(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false
        }

        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        val deniedPermanently = prefs.getBoolean(KEY_PERMISSION_DENIED_PERMANENTLY, false)

        if (deniedPermanently) return false

        // If we have permission already, no need to request
        if (hasNotificationPermission()) return false

        // Check if system allows showing permission dialog
        val activity = activityProvider.getCurrentActivity() ?: return false

        // If we haven't requested before, we can request
        if (!hasBeenRequestedBefore()) return true

        // If we have requested before, check if we should show rationale
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        )
    }

    override suspend fun openAppSettings(): Boolean {
        return try {
            // First try using activity context if available
            val activity = activityProvider.getCurrentActivity()
            if (activity != null) {
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", context.packageName, null)
                    // Don't add NEW_TASK flag when starting from Activity
                }
                activity.startActivity(intent)
                return true
            }

            // Fallback to application context
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback 1: Try app notification settings specifically
            try {
                val activity = activityProvider.getCurrentActivity()
                val intent = Intent().apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    } else {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    if (activity == null) {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }

                if (activity != null) {
                    activity.startActivity(intent)
                } else {
                    context.startActivity(intent)
                }
                true
            } catch (e2: Exception) {
                // Fallback 2: General settings
                try {
                    val activity = activityProvider.getCurrentActivity()
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        if (activity == null) {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    }

                    if (activity != null) {
                        activity.startActivity(intent)
                    } else {
                        context.startActivity(intent)
                    }
                    true
                } catch (e3: Exception) {
                    false
                }
            }
        }
    }

    private fun hasBeenRequestedBefore(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        return prefs.getBoolean("permission_requested", false)
    }

    private fun markPermissionRequested() {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        prefs.edit {putBoolean("permission_requested", true)}
    }

    private fun markPermissionDeniedPermanently() {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_PERMISSION_DENIED_PERMANENTLY, true)}
    }

    fun handlePermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        if (granted) {
            // Permission granted, clear any "denied permanently" flag
            val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_PERMISSION_DENIED_PERMANENTLY, false).apply()
        } else if (!shouldShowRationale) {
            // User denied and selected "Don't ask again"
            markPermissionDeniedPermanently()
        }
        markPermissionRequested()
    }
}