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
        val image = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
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

    override suspend fun scheduleNotification(config: NotificationConfig): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Cancel existing timer if any
                scheduledTasks[config.habitId]?.cancel()

                val timer = Timer("Habit_${config.habitId}", true)
                val task = object : TimerTask() {
                    override fun run() {
                        showNotification(config)
                    }
                }

                // Calculate initial delay and period
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val scheduledTime = LocalDateTime(now.date, config.time)

                val initialDelay = if (scheduledTime > now) {
                    scheduledTime.toInstant(TimeZone.currentSystemDefault()) -
                            now.toInstant(TimeZone.currentSystemDefault())
                } else {
                    // Schedule for next day
                    scheduledTime.date.plus(1, DateTimeUnit.DAY)
                        .atTime(config.time)
                        .toInstant(TimeZone.currentSystemDefault()) -
                            now.toInstant(TimeZone.currentSystemDefault())
                }

                // Schedule daily repetition
                timer.scheduleAtFixedRate(
                    task,
                    initialDelay.inWholeMilliseconds,
                    24 * 60 * 60 * 1000 // 24 hours in milliseconds
                )

                scheduledTasks[config.habitId] = timer
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun cancelNotification(habitId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                scheduledTasks[habitId]?.cancel()
                scheduledTasks.remove(habitId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun updateNotification(config: NotificationConfig): Result<Unit> {
        cancelNotification(config.habitId)
        return scheduleNotification(config)
    }

    override suspend fun checkPermission(): Boolean {
        // Desktop notifications don't require special permissions
        return SystemTray.isSupported()
    }

    override suspend fun requestPermission(): Boolean {
        return checkPermission()
    }

    private fun showNotification(config: NotificationConfig) {
        if (SystemTray.isSupported()) {
            trayIcon?.displayMessage(
                "Habit Reminder",
                config.message ?: "Time to complete your habit!",
                TrayIcon.MessageType.INFO
            )
        } else {
            // Fallback: Use native desktop notification via java-desktop
            showDesktopNotification(config)
        }
    }

    private fun showDesktopNotification(config: NotificationConfig) {
        try {
            val osName = System.getProperty("os.name").lowercase()

            when {
                osName.contains("mac") -> {
                    Runtime.getRuntime().exec(arrayOf(
                        "osascript", "-e",
                        "display notification \"${config.message}\" with title \"Habit Reminder\""
                    ))
                }
                osName.contains("linux") -> {
                    Runtime.getRuntime().exec(arrayOf(
                        "notify-send",
                        "Habit Reminder",
                        config.message ?: "Time to complete your habit!"
                    ))
                }
                osName.contains("windows") -> {
                    // Windows PowerShell notification
                    val command = """
                        [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
                        [Windows.UI.Notifications.ToastNotification, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
                        [Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] | Out-Null
                        
                        ${'$'}template = @"
                        <toast>
                            <visual>
                                <binding template="ToastText02">
                                    <text id="1">Habit Reminder</text>
                                    <text id="2">${config.message}</text>
                                </binding>
                            </visual>
                        </toast>
                        "@
                        
                        ${'$'}xml = New-Object Windows.Data.Xml.Dom.XmlDocument
                        ${'$'}xml.LoadXml(${'$'}template)
                        ${'$'}toast = [Windows.UI.Notifications.ToastNotification]::new(${'$'}xml)
                        [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier("HabitStreak").Show(${'$'}toast)
                    """.trimIndent()

                    Runtime.getRuntime().exec(arrayOf("powershell", "-Command", command))
                }
            }
        } catch (e: Exception) {
            println("Failed to show desktop notification: ${e.message}")
        }
    }

    fun cleanup() {
        scheduledTasks.values.forEach { it.cancel() }
        scheduledTasks.clear()

        SystemTray.getSystemTray().remove(trayIcon)
    }
}