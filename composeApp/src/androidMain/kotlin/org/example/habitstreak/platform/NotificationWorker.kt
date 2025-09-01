package org.example.habitstreak.platform

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.example.habitstreak.MainActivity

/**
 * Worker class for showing notifications
 * Follows Single Responsibility - only shows notifications
 */
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getString(AndroidNotificationScheduler.KEY_HABIT_ID)
            ?: return Result.failure()

        val message = inputData.getString(AndroidNotificationScheduler.KEY_MESSAGE)
            ?: "Time to complete your habit!"

        showNotification(habitId, message)

        return Result.success()
    }

    private fun showNotification(habitId: String, message: String) {
        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        // Create intent to open app when notification is clicked
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("habit_id", habitId)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            habitId.hashCode(),
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // Create notification
        val notification = NotificationCompat.Builder(
            applicationContext,
            AndroidNotificationScheduler.CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use system icon
            .setContentTitle("Habit Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_input_add,
                "Mark Complete",
                createCompleteActionIntent(habitId)
            )
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                "Snooze",
                createSnoozeActionIntent(habitId)
            )
            .build()

        // Show notification
        notificationManager.notify(habitId.hashCode(), notification)
    }

    private fun createCompleteActionIntent(habitId: String): PendingIntent {
        val intent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = "COMPLETE_HABIT"
            putExtra("habit_id", habitId)
        }

        return PendingIntent.getBroadcast(
            applicationContext,
            habitId.hashCode() + 1,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

    private fun createSnoozeActionIntent(habitId: String): PendingIntent {
        val intent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = "SNOOZE_HABIT"
            putExtra("habit_id", habitId)
        }

        return PendingIntent.getBroadcast(
            applicationContext,
            habitId.hashCode() + 2,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }
}