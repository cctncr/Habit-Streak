package org.example.habitstreak.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.service.NotificationScheduler
import java.util.concurrent.TimeUnit
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Android implementation of NotificationScheduler
 * Follows Single Responsibility - only handles scheduling, not permissions
 */
class AndroidNotificationScheduler(
    private val context: Context
) : NotificationScheduler {

    companion object {
        const val CHANNEL_ID = "habit_reminders"
        const val CHANNEL_NAME = "Habit Reminders"
        const val NOTIFICATION_WORK_TAG = "habit_notification_"
        const val KEY_HABIT_ID = "habit_id"
        const val KEY_HABIT_TITLE = "habit_title"
        const val KEY_MESSAGE = "message"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders for your habits"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun scheduleNotification(config: NotificationConfig): Result<Unit> {
        return try {
            val workRequest = createWorkRequest(config)

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NOTIFICATION_WORK_TAG + config.habitId,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelNotification(habitId: String): Result<Unit> {
        return try {
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

    @OptIn(ExperimentalTime::class)
    private fun createWorkRequest(config: NotificationConfig) =
        PeriodicWorkRequestBuilder<NotificationWorker>(
            24, TimeUnit.HOURS
        ).apply {
            setInputData(
                workDataOf(
                    KEY_HABIT_ID to config.habitId,
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