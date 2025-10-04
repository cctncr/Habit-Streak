package org.example.habitstreak.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.*
import org.example.habitstreak.domain.model.DayOfWeek
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.domain.service.NotificationPeriodValidator
import org.example.habitstreak.domain.usecase.notification.GetNotificationPreferencesUseCase
import platform.Foundation.*
import platform.UserNotifications.*
import kotlin.coroutines.resume
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * iOS implementation of NotificationScheduler
 * Following Single Responsibility - only handles scheduling
 * Now supports user sound preferences via GetNotificationPreferencesUseCase
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
class IOSNotificationScheduler(
    private val getNotificationPreferencesUseCase: GetNotificationPreferencesUseCase,
    private val periodValidator: NotificationPeriodValidator
) : NotificationScheduler {

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    @Volatile
    private var categoriesSetup = false

    private fun ensureCategoriesSetup() {
        if (!categoriesSetup) {
            setupNotificationCategories()
            categoriesSetup = true
        }
    }

    override suspend fun scheduleNotification(config: NotificationConfig): Result<Unit> {
        ensureCategoriesSetup()
        return try {
            // Validate config has required habit data
            if (config.habitFrequency == null || config.habitCreatedAt == null) {
                return Result.failure(Exception("NotificationConfig missing habitFrequency or habitCreatedAt"))
            }

            // Cancel previous notifications first
            cancelNotification(config.habitId)

            // Get user sound preferences
            val prefs = getNotificationPreferencesUseCase.execute()

            // Get notification days based on period
            val now = Clock.System.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val weekStartDate = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)

            val habitCreatedAt = config.habitCreatedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date

            // Use config data directly without creating domain objects
            val notificationDays = periodValidator.getNotificationDays(
                period = config.period,
                habitFrequency = config.habitFrequency,
                habitCreatedAt = habitCreatedAt,
                weekStartDate = weekStartDate
            )

            // Schedule notification for each day
            notificationDays.forEach { dayOfWeek ->
                scheduleForDayOfWeek(config, prefs, dayOfWeek)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelNotification(habitId: String): Result<Unit> {
        return try {
            // Cancel all day-specific notifications for this habit
            val identifiers = DayOfWeek.entries.map { day ->
                "habit_${habitId}_${day.name}"
            }

            notificationCenter.removePendingNotificationRequestsWithIdentifiers(identifiers)
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

    private suspend fun scheduleForDayOfWeek(
        config: NotificationConfig,
        prefs: GetNotificationPreferencesUseCase.Preferences,
        dayOfWeek: DayOfWeek
    ) {
        val content = UNMutableNotificationContent().apply {
            setTitle("Habit Reminder")
            setBody(config.message)

            // Apply sound preference based on user settings
            if (prefs.soundEnabled) {
                setSound(UNNotificationSound.defaultSound)
            } else {
                setSound(null) // Silent notification
            }

            setCategoryIdentifier("HABIT_REMINDER")
            setUserInfo(mapOf("habitId" to config.habitId))
        }

        // Create date components for specific day of week
        val dateComponents = createDateComponentsForDay(config.time, dayOfWeek)

        // Create trigger for weekly repeat on this day
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents,
            repeats = true // Weekly repeat
        )

        // Unique identifier for each day
        val identifier = "habit_${config.habitId}_${dayOfWeek.name}"

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = identifier,
            content = content,
            trigger = trigger
        )

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
    }

    private fun createDateComponentsForDay(time: LocalTime, dayOfWeek: DayOfWeek): NSDateComponents {
        return NSDateComponents().apply {
            setWeekday(mapDayOfWeekToNSWeekday(dayOfWeek).toLong())
            setHour(time.hour.toLong())
            setMinute(time.minute.toLong())
        }
    }

    private fun mapDayOfWeekToNSWeekday(dayOfWeek: DayOfWeek): Int {
        // NSCalendar weekday: Sunday = 1, Monday = 2, ..., Saturday = 7
        return when (dayOfWeek) {
            DayOfWeek.SUNDAY -> 1
            DayOfWeek.MONDAY -> 2
            DayOfWeek.TUESDAY -> 3
            DayOfWeek.WEDNESDAY -> 4
            DayOfWeek.THURSDAY -> 5
            DayOfWeek.FRIDAY -> 6
            DayOfWeek.SATURDAY -> 7
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