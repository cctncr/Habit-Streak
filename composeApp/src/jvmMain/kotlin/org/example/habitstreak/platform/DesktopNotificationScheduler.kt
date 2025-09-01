package org.example.habitstreak.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.service.NotificationScheduler
import java.awt.*
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import javax.swing.ImageIcon

/**
 * Desktop implementation of NotificationScheduler
 * Uses system tray for notifications
 */
class DesktopNotificationScheduler : NotificationScheduler {

    private val scheduledTasks = ConcurrentHashMap<String, Timer>()
    private var trayIcon: TrayIcon? = null

    init {
        setupSystemTray()
    }

    private fun setupSystemTray() {
        if (!SystemTray.isSupported()) {
            println("System tray is not supported on this platform")
            return
        }

        try {
            val tray = SystemTray.getSystemTray()
            val image = createDefaultIcon()

            trayIcon = TrayIcon(image, "HabitStreak").apply {
                isImageAutoSize = true
                addActionListener {
                    // Open app when tray icon is clicked
                }
            }

            tray.add(trayIcon)
        } catch (e: Exception) {
            println("Failed to setup system tray: ${e.message}")
        }
    }

    private fun createDefaultIcon(): Image {
        // Create a simple 16x16 icon
        val size = 16
        val image = java.awt.image.BufferedImage(
            size, size,
            java.awt.image.BufferedImage.TYPE_INT_ARGB
        )
        val g2d = image.createGraphics()

        // Draw a simple notification bell icon
        g2d.setRenderingHint(
            java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON
        )
        g2d.color = java.awt.Color(100, 100, 255)
        g2d.fillOval(2, 2, 12, 12)
        g2d.color = java.awt.Color.WHITE
        g2d.fillOval(4, 4, 8, 8)
        g2d.dispose()

        return image
    }

    override suspend fun scheduleNotification(config: NotificationConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Cancel existing timer if any
                cancelNotification(config.habitId)

                // Create new timer
                val timer = Timer("habit-${config.habitId}", true)

                // Calculate delay to first notification
                val now = Clock.System.now()
                val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
                var targetDateTime = today.atTime(config.time)

                // If time has passed today, schedule for tomorrow
                if (targetDateTime.toInstant(TimeZone.currentSystemDefault()) <= now) {
                    targetDateTime = today.plus(1, DateTimeUnit.DAY).atTime(config.time)
                }

                val delay = targetDateTime.toInstant(TimeZone.currentSystemDefault()) - now

                // Schedule daily task
                timer.scheduleAtFixedRate(
                    object : TimerTask() {
                        override fun run() {
                            showNotification(config)
                        }
                    },
                    delay.inWholeMilliseconds,
                    24 * 60 * 60 * 1000 // 24 hours in milliseconds
                )

                scheduledTasks[config.habitId] = timer
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun cancelNotification(habitId: String): Result<Unit> {
        return try {
            scheduledTasks.remove(habitId)?.cancel()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNotification(config: NotificationConfig): Result<Unit> {
        cancelNotification(config.habitId)
        return scheduleNotification(config)
    }

    override suspend fun cancelAllNotifications(): Result<Unit> {
        return try {
            scheduledTasks.values.forEach { it.cancel() }
            scheduledTasks.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isNotificationScheduled(habitId: String): Boolean {
        return scheduledTasks.containsKey(habitId)
    }

    private fun showNotification(config: NotificationConfig) {
        trayIcon?.displayMessage(
            "Habit Reminder",
            config.message,
            TrayIcon.MessageType.INFO
        )
    }
}