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
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For API < 33, notification permission is granted by default
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    override suspend fun requestNotificationPermission(): PermissionResult {
        // If already granted, return success
        if (hasNotificationPermission()) {
            return PermissionResult.Granted
        }

        // For API < 33, check if notifications are enabled in system settings
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return if (androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                PermissionResult.Granted
            } else {
                // Notifications disabled in system settings - must go to settings
                PermissionResult.DeniedPermanently
            }
        }

        if (!canRequestPermissionSync()) {
            return PermissionResult.DeniedPermanently
        }

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

            if (!shouldShowRationale && hasBeenRequestedBefore()) {
                PermissionResult.DeniedPermanently
            } else {
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
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback to main settings
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
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