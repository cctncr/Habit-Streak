package org.example.habitstreak.platform

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.example.habitstreak.di.appModule
import org.example.habitstreak.di.androidModule
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.util.DateProvider
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Handles notification actions
 * Following Open/Closed Principle - can be extended with new actions
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habit_id") ?: return

        // Ensure Koin is initialized
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                modules(appModule, androidModule)
            }
        }

        when (intent.action) {
            "COMPLETE_HABIT" -> handleCompleteHabit(context, habitId)
            "SNOOZE_HABIT" -> handleSnoozeHabit(context, habitId)
        }

        // Cancel notification
        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.cancel(habitId.hashCode())
    }

    private fun handleCompleteHabit(context: Context, habitId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val koin = GlobalContext.get()
                val habitRecordRepository = koin.get<HabitRecordRepository>()
                val dateProvider = koin.get<DateProvider>()

                // Mark habit as complete for today
                habitRecordRepository.toggleCompletion(
                    habitId = habitId,
                    date = dateProvider.today(),
                    targetCount = 1 // Default to 1 completion
                )

                // Show success toast or notification
                showCompletionNotification(context, habitId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleSnoozeHabit(context: Context, habitId: String) {
        // Schedule a new notification in 1 hour
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Implementation for snooze
                // You can reschedule the notification for later
                showSnoozeNotification(context, habitId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showCompletionNotification(context: Context, habitId: String) {
        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val notification = android.app.Notification.Builder(
            context,
            AndroidNotificationScheduler.CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Great job!")
            .setContentText("Habit completed successfully")
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            habitId.hashCode() + 1000, // Different ID to avoid conflicts
            notification
        )
    }

    private fun showSnoozeNotification(context: Context, habitId: String) {
        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val notification = android.app.Notification.Builder(
            context,
            AndroidNotificationScheduler.CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("Snoozed")
            .setContentText("We'll remind you again in 1 hour")
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            habitId.hashCode() + 2000, // Different ID
            notification
        )
    }
}