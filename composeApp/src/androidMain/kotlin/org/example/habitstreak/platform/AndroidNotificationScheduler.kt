package org.example.habitstreak.platform

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.unit.Constraints
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.datetime.*
import org.example.habitstreak.domain.model.NotificationConfig
import org.example.habitstreak.domain.service.NotificationScheduler
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import androidx.work.*
import kotlinx.datetime.*
import kotlin.time.ExperimentalTime

class AndroidNotificationScheduler(
    private val context: Context
) : NotificationScheduler {

    companion object {
        const val CHANNEL_ID = "habit_reminders"
        const val CHANNEL_NAME = "Habit Reminders"
        const val NOTIFICATION_WORK_TAG = "habit_notification_"
        const val PERMISSION_REQUEST_CODE = 1001
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

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
            WorkManager.getInstance(context).cancelUniqueWork(NOTIFICATION_WORK_TAG + habitId)
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    override suspend fun requestPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val activity = context as? Activity
            if (activity != null) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
        return checkPermission()
    }

    @OptIn(ExperimentalTime::class)
    private fun createWorkRequest(config: NotificationConfig): PeriodicWorkRequest {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val scheduledTime = LocalDateTime(now.date, config.time)

        val initialDelay = if (scheduledTime > now) {
            scheduledTime.toInstant(TimeZone.currentSystemDefault()) - now.toInstant(TimeZone.currentSystemDefault())
        } else {
            scheduledTime.date.plus(1, DateTimeUnit.DAY)
                .atTime(config.time)
                .toInstant(TimeZone.currentSystemDefault()) - now.toInstant(TimeZone.currentSystemDefault())
        }

        val inputData = workDataOf(
            "habitId" to config.habitId,
            "message" to config.message,
            "time" to config.time.toString()
        )

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        return PeriodicWorkRequestBuilder<HabitNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(NOTIFICATION_WORK_TAG + config.habitId)
            .setConstraints(constraints)
            .build()
    }
}