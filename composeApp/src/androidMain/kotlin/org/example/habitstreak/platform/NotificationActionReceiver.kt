package org.example.habitstreak.platform

import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.example.habitstreak.domain.usecase.notification.CompleteHabitFromNotificationUseCase
import org.example.habitstreak.domain.util.DateProvider
import org.koin.core.context.GlobalContext

/**
 * Handles notification actions
 * Following Open/Closed Principle - can be extended with new actions
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habit_id") ?: return

        val koinContext = GlobalContext.getOrNull()
        if (koinContext == null) {
            return
        }

        when (intent.action) {
            "COMPLETE_HABIT" -> handleCompleteHabit(context, habitId, koinContext)
        }

        // Cancel notification
        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.cancel(habitId.hashCode())
    }

    private fun handleCompleteHabit(context: Context, habitId: String, koinContext: org.koin.core.Koin) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val completeHabitUseCase = koinContext.get<CompleteHabitFromNotificationUseCase>()
                val dateProvider = koinContext.get<DateProvider>()

                // Use dedicated use case for consistent completion logic
                val result = completeHabitUseCase(
                    CompleteHabitFromNotificationUseCase.Params(
                        habitId = habitId,
                        date = dateProvider.today(),
                        count = 1,
                        note = "Completed from notification"
                    )
                )

                result.fold(
                    onSuccess = {
                        showCompletionNotification(context, habitId)
                    },
                    onFailure = { error ->
                        error.printStackTrace()
                        showErrorNotification(context, habitId)
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                showErrorNotification(context, habitId)
            }
        }
    }


    private fun showCompletionNotification(context: Context, habitId: String) {
        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        // Create intent to open habit detail when notification is clicked
        val intent = Intent(context, org.example.habitstreak.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("habit_id", habitId)
            putExtra("navigate_to_habit", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (habitId + "_complete").hashCode(),
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val notification = android.app.Notification.Builder(
            context,
            AndroidNotificationScheduler.CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Great job!")
            .setContentText("Habit completed successfully!")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Show completion notification briefly
        notificationManager.notify(
            (habitId + "_complete").hashCode(),
            notification
        )
    }


    private fun showErrorNotification(context: Context, habitId: String) {
        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val notification = android.app.Notification.Builder(
            context,
            AndroidNotificationScheduler.CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Error")
            .setContentText("Failed to complete habit. Please try again.")
            .setAutoCancel(true)
            .build()

        // Show error notification briefly
        notificationManager.notify(
            (habitId + "_error").hashCode(),
            notification
        )
    }
}