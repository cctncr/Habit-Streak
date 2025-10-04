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
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.domain.service.NotificationPeriodValidator
import org.example.habitstreak.domain.usecase.notification.GetNotificationPreferencesUseCase
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
class AndroidNotificationScheduler(
    private val context: Context,
    private val periodValidator: NotificationPeriodValidator,
    private val preferencesUseCase: GetNotificationPreferencesUseCase
) : NotificationScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val CHANNEL_ID = "habit_reminders"
        const val CHANNEL_NAME = "Habit Reminders"
        const val NOTIFICATION_WORK_TAG = "habit_notification_"
        const val KEY_HABIT_ID = "habit_id"
        const val KEY_HABIT_TITLE = "habit_title"
        const val KEY_MESSAGE = "message"
    }

    init {
        // Create channel with default settings on init
        // Will be recreated with user preferences when notification is scheduled
        runBlocking {
            createNotificationChannel(soundEnabled = true, vibrationEnabled = true)
        }
    }

    private suspend fun createNotificationChannel(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders for your habits"
                enableVibration(vibrationEnabled)
                setShowBadge(true)

                // Set sound based on preference
                if (soundEnabled) {
                    setSound(
                        android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                } else {
                    setSound(null, null)
                }
            }

            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun scheduleNotification(config: NotificationConfig): Result<Unit> {
        return try {
            // Validate config has required habit data
            if (config.habitFrequency == null || config.habitCreatedAt == null) {
                return Result.failure(Exception("NotificationConfig missing habitFrequency or habitCreatedAt"))
            }

            // Cancel previous notifications first
            cancelNotification(config.habitId)

            // Update notification channel with user preferences
            try {
                val prefs = preferencesUseCase.execute()
                createNotificationChannel(
                    soundEnabled = prefs.soundEnabled,
                    vibrationEnabled = prefs.vibrationEnabled
                )
            } catch (e: Exception) {
                // Fallback to config values if use case fails
                createNotificationChannel(
                    soundEnabled = config.soundEnabled,
                    vibrationEnabled = config.vibrationEnabled
                )
            }

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

            // Get habit title for notification (fallback to habitId if not available)
            val habitTitle = config.message.substringAfter("Time to complete: ", config.habitId)

            // Schedule alarm for each notification day
            notificationDays.forEach { dayOfWeek ->
                if (canScheduleExactAlarms()) {
                    scheduleForDayOfWeek(config, habitTitle, dayOfWeek)
                } else {
                    // Fallback to WorkManager for this day
                    scheduleWithWorkManager(config)
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

    override suspend fun updateNotification(config: NotificationConfig): Result<Unit> {
        // Cancel existing and reschedule
        cancelNotification(config.habitId)
        return scheduleNotification(config)
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
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Find next occurrence of this day of week
        val targetDate = findNextDayOfWeek(today, dayOfWeek)
        val targetDateTime = targetDate.atTime(config.time)

        val triggerTime = targetDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val pendingIntent = createAlarmPendingIntent(config, habitTitle, dayOfWeek)

        // Use setRepeating for weekly repeat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                AlarmManager.INTERVAL_DAY * 7, // Repeat weekly
                pendingIntent
            )
        } else {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
            )
        }
    }

    private fun findNextDayOfWeek(fromDate: LocalDate, targetDay: DayOfWeek): LocalDate {
        val kotlinxTargetDay = when (targetDay) {
            DayOfWeek.MONDAY -> KotlinxDayOfWeek.MONDAY
            DayOfWeek.TUESDAY -> KotlinxDayOfWeek.TUESDAY
            DayOfWeek.WEDNESDAY -> KotlinxDayOfWeek.WEDNESDAY
            DayOfWeek.THURSDAY -> KotlinxDayOfWeek.THURSDAY
            DayOfWeek.FRIDAY -> KotlinxDayOfWeek.FRIDAY
            DayOfWeek.SATURDAY -> KotlinxDayOfWeek.SATURDAY
            DayOfWeek.SUNDAY -> KotlinxDayOfWeek.SUNDAY
        }

        var date = fromDate
        while (date.dayOfWeek != kotlinxTargetDay) {
            date = date.plus(1, DateTimeUnit.DAY)
        }
        return date
    }

    private suspend fun scheduleWithWorkManager(config: NotificationConfig) {
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