package org.example.habitstreak.platform

import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.*
import platform.UserNotifications.*
import platform.Foundation.*
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.service.NotificationScheduler
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IOSNotificationScheduler : NotificationScheduler {

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun scheduleNotification(config: NotificationConfig): Result<Unit> {
        return try {
            // Create notification content
            val content = UNMutableNotificationContent().apply {
                setTitle("Habit Reminder")
                setBody(config.message ?: "Time to complete your habit!")
                setSound(UNNotificationSound.defaultSound())
                setBadge(1)
                setCategoryIdentifier("HABIT_REMINDER")
                setUserInfo(mapOf("habitId" to config.habitId))
            }

            // Create trigger based on time
            val dateComponents = createDateComponents(config.time)
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
            suspendCancellableCoroutine<Unit> { continuation ->
                notificationCenter.addNotificationRequest(request) { error ->
                    if (error != null) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resume(Unit)
                    }
                }
            }

            Result.success(Unit)
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

    override suspend fun checkPermission(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                val isAuthorized = settings?.authorizationStatus ==
                        UNAuthorizationStatusAuthorized
                continuation.resume(isAuthorized)
            }
        }
    }

    override suspend fun requestPermission(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val options = UNAuthorizationOptionAlert or
                    UNAuthorizationOptionBadge or
                    UNAuthorizationOptionSound

            notificationCenter.requestAuthorizationWithOptions(options) { granted, _ ->
                continuation.resume(granted)
            }
        }
    }

    private fun createDateComponents(time: LocalTime): NSDateComponents {
        return NSDateComponents().apply {
            setHour(time.hour.toLong())
            setMinute(time.minute.toLong())
        }
    }

    fun setupNotificationCategories() {
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