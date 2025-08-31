package org.example.habitstreak.platform

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlin.jvm.java
import kotlin.random.Random

class HabitNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getString("habitId") ?: return Result.failure()
        val message = inputData.getString("message") ?: "Time to complete your habit!"

        showNotification(habitId, message)

        return Result.success()
    }

    private fun showNotification(habitId: String, message: String) {
        // Create main intent
        val mainIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("habitId", habitId)
            }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            habitId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, AndroidNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Habit Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_send,
                "Mark Complete",
                createCompleteIntent(habitId)
            )
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                "Snooze",
                createSnoozeIntent(habitId)
            )
            .build()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(applicationContext)
                    .notify(habitId.hashCode(), notification)
            }
        } else {
            NotificationManagerCompat.from(applicationContext)
                .notify(habitId.hashCode(), notification)
        }
    }

    private fun createCompleteIntent(habitId: String): PendingIntent {
        val intent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = "COMPLETE_HABIT"
            putExtra("habitId", habitId)
        }

        return PendingIntent.getBroadcast(
            applicationContext,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createSnoozeIntent(habitId: String): PendingIntent {
        val intent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = "SNOOZE_HABIT"
            putExtra("habitId", habitId)
        }

        return PendingIntent.getBroadcast(
            applicationContext,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
