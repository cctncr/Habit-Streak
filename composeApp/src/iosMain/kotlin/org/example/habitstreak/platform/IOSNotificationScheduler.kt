package org.example.habitstreak.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.*
import org.example.habitstreak.domain.model.DayOfWeek
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.domain.service.NotificationPeriodValidator
import org.example.habitstreak.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import platform.Foundation.*
import platform.UserNotifications.*
import kotlin.coroutines.resume
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * iOS implementation of NotificationScheduler
 * Following Single Responsibility - only handles scheduling
 * Now supports user sound preferences via PreferencesRepository
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
class IOSNotificationScheduler(
    private val preferencesRepository: PreferencesRepository,
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

    override suspend fun scheduleNotification(
        config: NotificationConfig,
        habitFrequency: HabitFrequency,
        habitCreatedAt: Instant
    ): Result<Unit> {
        ensureCategoriesSetup()
        return try {
            // Cancel previous notifications first
            cancelNotification(config.habitId)

            // Get user sound preferences
            val soundEnabled = preferencesRepository.isSoundEnabled().first()

            // Get notification days based on period
            val now = Clock.System.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val weekStartDate = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)

            val habitCreatedDate = habitCreatedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date

            // Use passed parameters instead of config fields
            val notificationDays = periodValidator.getNotificationDays(
                period = config.period,
                habitFrequency = habitFrequency,    // ✅ From parameter
                habitCreatedAt = habitCreatedDate,  // ✅ From parameter
                weekStartDate = weekStartDate
            )

            // Schedule notification for each day with proper error handling
            // ✅ FIX: Use for loop to properly await suspend function calls
            for (dayOfWeek in notificationDays) {
                val result = scheduleForDayOfWeek(config, soundEnabled, dayOfWeek)
                if (result.isFailure) {
                    // Return early on first failure (fail-fast)
                    return result
                }
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

    override suspend fun updateNotification(
        config: NotificationConfig,
        habitFrequency: HabitFrequency,
        habitCreatedAt: Instant
    ): Result<Unit> {
        cancelNotification(config.habitId)
        return scheduleNotification(config, habitFrequency, habitCreatedAt)
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
                    // ✅ FIX: Use startsWith since identifiers include day of week
                    // Format: "habit_{habitId}_{dayOfWeek}"
                    (request as? UNNotificationRequest)?.identifier?.startsWith("habit_$habitId") == true
                } ?: false
                continuation.resume(isScheduled)
            }
        }
    }

    private suspend fun scheduleForDayOfWeek(
        config: NotificationConfig,
        soundEnabled: Boolean,
        dayOfWeek: DayOfWeek
    ): Result<Unit> {
        val content = UNMutableNotificationContent().apply {
            setTitle("Habit Reminder")
            setBody(config.message)

            // Apply sound preference based on user settings
            if (soundEnabled) {
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

        return suspendCancellableCoroutine { continuation ->
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