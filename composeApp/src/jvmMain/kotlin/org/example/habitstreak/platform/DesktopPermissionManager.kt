package org.example.habitstreak.platform

import org.example.habitstreak.domain.service.PermissionManager
import org.example.habitstreak.domain.service.PermissionResult
import java.awt.Desktop
import java.awt.SystemTray
import java.net.URI

/**
 * Desktop permission manager
 * Desktop notifications don't require explicit permission
 * Following Single Responsibility Principle
 */
class DesktopPermissionManager : PermissionManager {

    override suspend fun hasNotificationPermission(): Boolean {
        // Desktop notifications don't require explicit permission
        // Check if system tray is supported as a proxy for notification capability
        return SystemTray.isSupported()
    }

    override suspend fun requestNotificationPermission(): PermissionResult {
        // No explicit permission needed on desktop
        return if (SystemTray.isSupported()) {
            PermissionResult.Granted
        } else {
            PermissionResult.DeniedPermanently
        }
    }

    override suspend fun canRequestPermission(): Boolean {
        // No permission request needed on desktop
        return false
    }

    override suspend fun openAppSettings(): Boolean {
        return try {
            // Try to open system notification settings
            val osName = System.getProperty("os.name").lowercase()

            when {
                osName.contains("windows") -> {
                    // Open Windows notification settings
                    Runtime.getRuntime().exec("ms-settings:notifications")
                    true
                }
                osName.contains("mac") -> {
                    // Open macOS notification settings
                    Runtime.getRuntime().exec(arrayOf(
                        "open",
                        "x-apple.systempreferences:com.apple.preference.notifications"
                    ))
                    true
                }
                osName.contains("linux") -> {
                    // Try to open Linux notification settings (varies by desktop environment)
                    try {
                        Runtime.getRuntime().exec("gnome-control-center notifications")
                        true
                    } catch (e: Exception) {
                        try {
                            Runtime.getRuntime().exec("systemsettings5 kcm_notifications")
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                }
                else -> false
            }
        } catch (e: Exception) {
            // Fallback: try to open general system settings
            try {
                if (Desktop.isDesktopSupported()) {
                    val desktop = Desktop.getDesktop()
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        val osName = System.getProperty("os.name").lowercase()
                        when {
                            osName.contains("windows") -> {
                                desktop.browse(URI("ms-settings:"))
                                true
                            }
                            else -> false
                        }
                    } else false
                } else false
            } catch (e: Exception) {
                false
            }
        }
    }
}