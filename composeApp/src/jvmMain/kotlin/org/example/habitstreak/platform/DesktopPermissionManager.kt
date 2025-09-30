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
            PermissionResult.GloballyDisabled
        }
    }

    override suspend fun canRequestPermission(): Boolean {
        // No permission request needed on desktop
        return false
    }

    override suspend fun openAppSettings(): Boolean {
        return try {
            val osName = System.getProperty("os.name").lowercase()

            when {
                osName.contains("windows") -> {
                    openWindowsSettings()
                }
                osName.contains("mac") -> {
                    openMacSettings()
                }
                osName.contains("linux") -> {
                    openLinuxSettings()
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun openWindowsSettings(): Boolean {
        return try {
            // Try notification settings first
            ProcessBuilder("cmd", "/c", "start", "ms-settings:notifications").start()
            true
        } catch (e: Exception) {
            try {
                // Fallback to general settings
                ProcessBuilder("cmd", "/c", "start", "ms-settings:").start()
                true
            } catch (e: Exception) {
                try {
                    // Last resort: control panel
                    Runtime.getRuntime().exec("control")
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    private fun openMacSettings(): Boolean {
        return try {
            // Open macOS notification settings
            ProcessBuilder("open", "x-apple.systempreferences:com.apple.preference.notifications").start()
            true
        } catch (e: Exception) {
            try {
                // Fallback to system preferences
                ProcessBuilder("open", "/System/Applications/System Preferences.app").start()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun openLinuxSettings(): Boolean {
        return try {
            // Try GNOME settings
            ProcessBuilder("gnome-control-center", "notifications").start()
            true
        } catch (e: Exception) {
            try {
                // Try KDE settings
                ProcessBuilder("systemsettings5", "kcm_notifications").start()
                true
            } catch (e: Exception) {
                try {
                    // Try general settings
                    ProcessBuilder("gnome-control-center").start()
                    true
                } catch (e: Exception) {
                    try {
                        ProcessBuilder("systemsettings5").start()
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }
        }
    }
}