package org.example.habitstreak.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.*
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.service.NotificationScheduler
import platform.Foundation.*
import platform.UserNotifications.*
import kotlin.coroutines.resume

/**
 * iOS implementation of NotificationScheduler
 * Following Single Responsibility - only handles scheduling
 */
@OptIn(ExperimentalForeignApi::class)
class IOSNotificationScheduler : NotificationScheduler {

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    init {
        setupNotificationCategories()
    }

    override suspend fun scheduleNotification(config: NotificationConfig): Result<Unit> {
        return try {
            val content = UNMutableNotificationContent().apply {
                setTitle("Habit Reminder")
                setBody(config.message)
                setSound(UNNotificationSound.defaultSound)
                setCategoryIdentifier("HABIT_REMINDER")
                setUserInfo(mapOf("habitId" to config.habitId))
            }

            // Create date components for daily notification
            val dateComponents = createDateComponents(config.time)

            // Create trigger for daily repeat
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents,
                repeats = true
            )

            // Create request
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "habit_${config.habitId}",
                content = content,
                trigger = trigger
            )

            // Schedule notification
            suspendCancellableCoroutine { continuation ->
                notificationCenter.addNotificationRequest(request) { error ->
                    if (error == null) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        continuation.resume(
                            Result.failure(Exception(error.localizedDescription))
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelNotification(habitId: String): Result<Unit> {
        return try {
            notificationCenter.removePendingNotificationRequestsWithIdentifiers(
                listOf("habit_$habitId")
            )
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
            notificationCenter.removeAllPendingNotificationRequests()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isNotificationScheduled(habitId: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            notificationCenter.getPendingNotificationRequestsWithCompletionHandler { requests ->
                val isScheduled = requests?.any { request ->
                    (request as? UNNotificationRequest)?.identifier == "habit_$habitId"
                } ?: false
                continuation.resume(isScheduled)
            }
        }
    }

    private fun createDateComponents(time: LocalTime): NSDateComponents {
        return NSDateComponents().apply {
            setHour(time.hour.toLong())
            setMinute(time.minute.toLong())
        }
    }

    private fun setupNotificationCategories() {
        val completeAction = UNNotificationAction.actionWithIdentifier(
            identifier = "COMPLETE_ACTION",
            title = "Mark Complete",
            options = UNNotificationActionOptionForeground
        )

        val snoozeAction = UNNotificationAction.actionWithIdentifier(
            identifier = "SNOOZE_ACTION",
            title = "Snooze",
            options = UNNotificationActionOptionNone
        )

        val category = UNNotificationCategory.categoryWithIdentifier(
            identifier = "HABIT_REMINDER",
            actions = listOf(completeAction, snoozeAction),
            intentIdentifiers = emptyList<String>(),
            options = UNNotificationCategoryOptionCustomDismissAction
        )

        notificationCenter.setNotificationCategories(setOf(category))
    }
}