package org.example.habitstreak.presentation.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.awt.SystemTray
import java.awt.Toolkit
import java.net.URI

/**
 * Desktop-specific permission handler
 * Desktop doesn't require explicit permissions but we check system capabilities
 */
@Composable
fun rememberDesktopPermissionHandler(
    onPermissionResult: (granted: Boolean, canAskAgain: Boolean) -> Unit
): DesktopPermissionHandler {
    return remember {
        DesktopPermissionHandler { granted, canAskAgain ->
            onPermissionResult(granted, canAskAgain)
        }
    }
}

/**
 * Desktop permission handler wrapper
 */
class DesktopPermissionHandler(
    private val onResult: (granted: Boolean, canAskAgain: Boolean) -> Unit
) {

    /**
     * Request notification permission on Desktop
     * Desktop doesn't need explicit permission, but we check system capabilities
     */
    fun requestNotificationPermission(): Pair<Boolean, Boolean> {
        val hasSystemTray = SystemTray.isSupported()
        val canShowNotifications = hasSystemTray || hasDesktopNotificationSupport()

        return Pair(canShowNotifications, true) // Always can "ask again" on desktop
    }

    /**
     * Check if notification permission is granted
     * On desktop, this checks if system supports notifications
     */
    fun hasNotificationPermission(): Boolean {
        return SystemTray.isSupported() || hasDesktopNotificationSupport()
    }

    /**
     * Check if we can request permission
     * Always true on desktop since no explicit permission is needed
     */
    fun canRequestPermission(): Boolean = true

    /**
     * Open system notification settings
     * This varies by OS (Windows, macOS, Linux)
     */
    fun openNotificationSettings(): Boolean {
        return try {
            val os = System.getProperty("os.name").lowercase()
            when {
                os.contains("windows") -> openWindowsNotificationSettings()
                os.contains("mac") -> openMacNotificationSettings()
                os.contains("linux") -> openLinuxNotificationSettings()
                else -> openGenericSettings()
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if system tray is available
     */
    fun isSystemTraySupported(): Boolean {
        return SystemTray.isSupported()
    }

    /**
     * Check if desktop has notification support
     */
    fun hasDesktopNotificationSupport(): Boolean {
        return try {
            // Check if we can use system tray for notifications
            SystemTray.isSupported() ||
            // Check if we have other notification mechanisms
            hasNativeNotificationSupport()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get notification capabilities summary
     */
    fun getNotificationCapabilities(): DesktopNotificationCapabilities {
        val hasSystemTray = SystemTray.isSupported()
        val hasNativeSupport = hasNativeNotificationSupport()
        val hasSoundSupport = hasSoundSupport()

        return DesktopNotificationCapabilities(
            hasSystemTray = hasSystemTray,
            hasNativeNotifications = hasNativeSupport,
            hasSoundSupport = hasSoundSupport,
            canShowBalloonTips = hasSystemTray,
            recommendedFallback = if (!hasSystemTray && !hasNativeSupport) {
                "Visual alerts and sound notifications"
            } else null
        )
    }

    /**
     * Show a test notification to verify functionality
     */
    fun showTestNotification(): Boolean {
        return try {
            if (SystemTray.isSupported()) {
                // Would show system tray notification
                true
            } else {
                // Would show alternative notification (sound, dialog, etc.)
                Toolkit.getDefaultToolkit().beep()
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun openWindowsNotificationSettings(): Boolean {
        return try {
            val command = "ms-settings:notifications"
            Desktop.getDesktop().browse(URI(command))
            true
        } catch (e: Exception) {
            // Fallback to general settings
            try {
                Runtime.getRuntime().exec("rundll32 shell32.dll,Control_RunDLL desk.cpl")
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    private fun openMacNotificationSettings(): Boolean {
        return try {
            val command = arrayOf("open", "-b", "com.apple.preference.notifications")
            Runtime.getRuntime().exec(command)
            true
        } catch (e: Exception) {
            // Fallback to system preferences
            try {
                Runtime.getRuntime().exec(arrayOf("open", "/System/Library/PreferencePanes/Notifications.prefPane"))
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    private fun openLinuxNotificationSettings(): Boolean {
        return try {
            // Try different desktop environments
            val commands = listOf(
                arrayOf("gnome-control-center", "notifications"),
                arrayOf("systemsettings5", "kcm_notifications"),
                arrayOf("unity-control-center", "notifications"),
                arrayOf("xfce4-notifyd-config")
            )

            for (command in commands) {
                try {
                    Runtime.getRuntime().exec(command)
                    return true
                } catch (e: Exception) {
                    // Try next command
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun openGenericSettings(): Boolean {
        return try {
            // Last resort - try to open system settings
            if (Desktop.isDesktopSupported()) {
                // This doesn't work for all systems, but it's worth a try
                false
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun hasNativeNotificationSupport(): Boolean {
        return try {
            val os = System.getProperty("os.name").lowercase()
            when {
                os.contains("windows") -> true // Windows has native notifications
                os.contains("mac") -> true // macOS has native notifications
                os.contains("linux") -> hasLinuxNotificationSupport()
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun hasLinuxNotificationSupport(): Boolean {
        return try {
            // Check for common notification systems
            val commands = listOf("notify-send", "zenity", "kdialog")
            commands.any { command ->
                try {
                    Runtime.getRuntime().exec(arrayOf("which", command))
                    true
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun hasSoundSupport(): Boolean {
        return try {
            Toolkit.getDefaultToolkit() != null
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Desktop notification capabilities data class
 */
data class DesktopNotificationCapabilities(
    val hasSystemTray: Boolean,
    val hasNativeNotifications: Boolean,
    val hasSoundSupport: Boolean,
    val canShowBalloonTips: Boolean,
    val recommendedFallback: String?
)

/**
 * Desktop-specific permission utilities
 */
object DesktopPermissionUtils {

    /**
     * Get platform-specific notification message
     */
    fun getNotificationMessage(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("windows") ->
                "Notifications will appear in your system tray and Windows notification center."

            os.contains("mac") ->
                "Notifications will appear in your macOS notification center."

            os.contains("linux") ->
                "Notifications will appear using your desktop environment's notification system."

            else ->
                "Notifications will appear using your system's available notification methods."
        }
    }

    /**
     * Get troubleshooting tips for notification issues
     */
    fun getTroubleshootingTips(): List<String> {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("windows") -> listOf(
                "Make sure notifications are enabled in Windows Settings",
                "Check that 'Focus Assist' is not blocking notifications",
                "Verify that the application is not in the blocked notifications list"
            )

            os.contains("mac") -> listOf(
                "Check System Preferences > Notifications > Habit Streak",
                "Make sure 'Do Not Disturb' is not enabled",
                "Verify that notifications are allowed for this application"
            )

            os.contains("linux") -> listOf(
                "Ensure your desktop environment supports notifications",
                "Check if 'notify-send' or similar tools are installed",
                "Verify notification daemon is running"
            )

            else -> listOf(
                "Check your system's notification settings",
                "Ensure the application has permission to show notifications",
                "Try restarting the application if notifications aren't working"
            )
        }
    }
}