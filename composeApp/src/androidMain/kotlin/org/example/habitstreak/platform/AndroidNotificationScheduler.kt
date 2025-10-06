package org.example.habitstreak.platform

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.example.habitstreak.domain.model.DayOfWeek
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.domain.service.NotificationPeriodValidator
import org.example.habitstreak.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import java.util.concurrent.TimeUnit
import kotlinx.datetime.*
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DayOfWeek as KotlinxDayOfWeek

/**
 * Android implementation of NotificationScheduler
 * Uses AlarmManager for exact timing with WorkManager as fallback
 * Follows Single Responsibility - only handles scheduling, not permissions
 */
@OptIn(ExperimentalTime::class)
class AndroidNotificationScheduler(
    private val context: Context,
    private val periodValidator: NotificationPeriodValidator,
    private val preferencesRepository: PreferencesRepository
) : NotificationScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        // ✅ Channel ID v3 - IMPORTANCE_DEFAULT for proper sound control
        const val CHANNEL_ID = "habit_reminders_v3"
        const val CHANNEL_NAME = "Habit Reminders"
        // Old channel IDs for cleanup
        private const val OLD_CHANNEL_ID_V1 = "habit_reminders"
        private const val OLD_CHANNEL_ID_V2 = "habit_reminders_v2"
        const val NOTIFICATION_WORK_TAG = "habit_notification_"
        const val KEY_HABIT_ID = "habit_id"
        const val KEY_HABIT_TITLE = "habit_title"
        const val KEY_MESSAGE = "message"
    }

    init {
        // ✅ FIX: Create notification channel ONCE with IMPORTANCE_DEFAULT
        // Sound/vibration will be controlled at notification level using setSilent()
        // because NotificationChannel is immutable after creation
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            // ✅ FIX: Delete old channels (v1 and v2)
            deleteOldChannels(notificationManager)

            // Only create if doesn't exist (channel is immutable)
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT  // ✅ DEFAULT: Allows sound when enabled
                ).apply {
                    description = "Daily reminders for your habits"
                    setShowBadge(true)
                    enableLights(true)
                    // Sound control strategy:
                    // - IMPORTANCE_DEFAULT allows notifications to make sound
                    // - When sound is OFF: Use setSilent(true) to override channel (makes silent)
                    // - When sound is ON: Channel default sound plays (or explicit setSound())
                    // - setSilent() has highest priority and overrides channel importance
                }

                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Delete old notification channels (v1 and v2)
     * SOLID: Single Responsibility - handles only channel cleanup
     */
    private fun deleteOldChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Delete v1 channel (IMPORTANCE_HIGH)
                val oldChannelV1 = notificationManager.getNotificationChannel(OLD_CHANNEL_ID_V1)
                if (oldChannelV1 != null) {
                    notificationManager.deleteNotificationChannel(OLD_CHANNEL_ID_V1)
                    println("🔔 SCHEDULER: Deleted old channel '$OLD_CHANNEL_ID_V1' (v1 - IMPORTANCE_HIGH)")
                }

                // Delete v2 channel (IMPORTANCE_LOW - the broken one)
                val oldChannelV2 = notificationManager.getNotificationChannel(OLD_CHANNEL_ID_V2)
                if (oldChannelV2 != null) {
                    notificationManager.deleteNotificationChannel(OLD_CHANNEL_ID_V2)
                    println("🔔 SCHEDULER: Deleted old channel '$OLD_CHANNEL_ID_V2' (v2 - IMPORTANCE_LOW)")
                }
            } catch (e: Exception) {
                println("⚠️ SCHEDULER: Failed to delete old channels: ${e.message}")
                // Non-critical error - continue with new channel creation
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun scheduleNotification(
        config: NotificationConfig,
        habitFrequency: HabitFrequency,
        habitCreatedAt: Instant
    ): Result<Unit> {
        return try {
            // Cancel previous notifications first
            cancelNotification(config.habitId)

            // Get habit title from repository
            val habitTitle = try {
                val koinContext = GlobalContext.getOrNull()
                koinContext?.get<HabitRepository>()?.getHabitById(config.habitId)?.getOrNull()?.title
            } catch (e: Exception) {
                null
            } ?: "Habit"

            // Note: Sound/vibration preferences are applied at notification display time
            // (in AlarmReceiver/NotificationDisplayHelper), not at scheduling time,
            // because NotificationChannel is immutable after creation

            // Get notification days based on period
            val now = Clock.System.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val weekStartDate = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)

            val habitCreatedDate = habitCreatedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date

            // Use passed parameters instead of config fields
            val notificationDays = periodValidator.getNotificationDays(
                period = config.period,
                habitFrequency = habitFrequency,     // ✅ From parameter
                habitCreatedAt = habitCreatedDate,   // ✅ From parameter
                weekStartDate = weekStartDate
            )

            // Schedule alarm for each notification day
            notificationDays.forEach { dayOfWeek ->
                if (canScheduleExactAlarms()) {
                    scheduleForDayOfWeek(config, habitTitle, dayOfWeek)
                } else {
                    // Fallback to WorkManager for this day
                    scheduleWithWorkManager(config, habitTitle)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelNotification(habitId: String): Result<Unit> {
        return try {
            // Cancel all day-specific alarms for this habit
            DayOfWeek.entries.forEach { dayOfWeek ->
                cancelAlarmForDayOfWeek(habitId, dayOfWeek)
            }

            // Also cancel WorkManager
            WorkManager.getInstance(context)
                .cancelUniqueWork(NOTIFICATION_WORK_TAG + habitId)
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
        // Cancel existing and reschedule
        cancelNotification(config.habitId)
        return scheduleNotification(config, habitFrequency, habitCreatedAt)
    }

    override suspend fun cancelAllNotifications(): Result<Unit> {
        return try {
            // Cancel all alarms and work requests
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(NOTIFICATION_WORK_TAG)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isNotificationScheduled(habitId: String): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(NOTIFICATION_WORK_TAG + habitId)
                .get()

            workInfos.any { !it.state.isFinished }
        } catch (e: Exception) {
            false
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun scheduleForDayOfWeek(
        config: NotificationConfig,
        habitTitle: String,
        dayOfWeek: DayOfWeek
    ) {
        val now = Clock.System.now()
        val nowLocal = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val today = nowLocal.date

        // ✅ FIX: Smart date selection - check if we can schedule today
        val kotlinxTargetDay = convertDayOfWeek(dayOfWeek)
        val targetDate = if (today.dayOfWeek == kotlinxTargetDay) {
            // Today is the target day - check if time hasn't passed yet
            val targetTimeToday = today.atTime(config.time)
            val targetInstant = targetTimeToday.toInstant(TimeZone.currentSystemDefault())

            if (now < targetInstant) {
                // Time hasn't passed - schedule for today!
                today
            } else {
                // Time already passed - schedule for next week
                today.plus(7, DateTimeUnit.DAY)
            }
        } else {
            // Not target day - find next occurrence
            findNextDayOfWeek(today, dayOfWeek)
        }

        val targetDateTime = targetDate.atTime(config.time)
        val triggerTime = targetDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val pendingIntent = createAlarmPendingIntent(config, habitTitle, dayOfWeek)

        // ✅ FIX: Use exact alarm APIs (setRepeating is inexact on modern Android)
        // Schedule exact alarm - AlarmReceiver will reschedule after trigger
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12+ (API 31+) - Use setExactAndAllowWhileIdle
                // This allows alarm to fire even in Doze mode
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // Fallback to WorkManager if exact alarms not allowed
                    scheduleWithWorkManager(config, habitTitle)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6+ (API 23+) - Use setExactAndAllowWhileIdle
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                // Android 4.4+ (API 19+) - Use setExact
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            else -> {
                // Below Android 4.4 - Use set
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        }
    }

    private fun convertDayOfWeek(targetDay: DayOfWeek): KotlinxDayOfWeek {
        return when (targetDay) {
            DayOfWeek.MONDAY -> KotlinxDayOfWeek.MONDAY
            DayOfWeek.TUESDAY -> KotlinxDayOfWeek.TUESDAY
            DayOfWeek.WEDNESDAY -> KotlinxDayOfWeek.WEDNESDAY
            DayOfWeek.THURSDAY -> KotlinxDayOfWeek.THURSDAY
            DayOfWeek.FRIDAY -> KotlinxDayOfWeek.FRIDAY
            DayOfWeek.SATURDAY -> KotlinxDayOfWeek.SATURDAY
            DayOfWeek.SUNDAY -> KotlinxDayOfWeek.SUNDAY
        }
    }

    private fun findNextDayOfWeek(fromDate: LocalDate, targetDay: DayOfWeek): LocalDate {
        val kotlinxTargetDay = convertDayOfWeek(targetDay)

        // Find NEXT occurrence (excluding today)
        // Used for finding next week's occurrence when today is already the target day but time passed
        var date = fromDate.plus(1, DateTimeUnit.DAY)
        while (date.dayOfWeek != kotlinxTargetDay) {
            date = date.plus(1, DateTimeUnit.DAY)
        }
        return date
    }

    private suspend fun scheduleWithWorkManager(config: NotificationConfig, habitTitle: String) {
        val workRequest = createWorkRequest(config)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOTIFICATION_WORK_TAG + config.habitId,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun cancelAlarmForDayOfWeek(habitId: String, dayOfWeek: DayOfWeek) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val requestCode = (habitId.hashCode() * 10) + dayOfWeek.ordinal

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private suspend fun createAlarmPendingIntent(
        config: NotificationConfig,
        habitTitle: String,
        dayOfWeek: DayOfWeek
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_HABIT_ID, config.habitId)
            putExtra(AlarmReceiver.EXTRA_HABIT_TITLE, habitTitle)
            putExtra(AlarmReceiver.EXTRA_MESSAGE, config.message)
        }

        // Unique request code for each day of week
        val requestCode = (config.habitId.hashCode() * 10) + dayOfWeek.ordinal

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun createWorkRequest(config: NotificationConfig) =
        PeriodicWorkRequestBuilder<NotificationWorker>(
            24, TimeUnit.HOURS
        ).apply {
            // Extract habit title from message or use habitId as fallback
            val habitTitle = config.message.substringAfter("Time to complete: ", config.habitId)

            setInputData(
                workDataOf(
                    KEY_HABIT_ID to config.habitId,
                    KEY_HABIT_TITLE to habitTitle,
                    KEY_MESSAGE to config.message
                )
            )

            // Calculate initial delay to notification time
            val now = Clock.System.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            var targetDateTime = today.atTime(config.time)

            // If time has passed today, schedule for tomorrow
            if (targetDateTime.toInstant(TimeZone.currentSystemDefault()) <= now) {
                targetDateTime = today.plus(1, DateTimeUnit.DAY).atTime(config.time)
            }

            val delay = targetDateTime.toInstant(TimeZone.currentSystemDefault()) - now
            setInitialDelay(delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)

            addTag(NOTIFICATION_WORK_TAG)
            addTag(NOTIFICATION_WORK_TAG + config.habitId)
        }.build()
}