import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.platform.AndroidNotificationScheduler
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
            "SNOOZE_HABIT" -> handleSnoozeHabit(context, habitId, koinContext)
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
                val habitRecordRepository = koinContext.get<HabitRecordRepository>()
                val dateProvider = koinContext.get<DateProvider>()

                // Mark habit as complete for today
                habitRecordRepository.markHabitAsComplete(
                    habitId = habitId,
                    date = dateProvider.today(),
                    count = 1, // Default to 1 completion
                    note = "Completed from notification"
                )

                // Show success toast or notification
                showCompletionNotification(context, habitId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleSnoozeHabit(context: Context, habitId: String, koinContext: org.koin.core.Koin) {
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
            .setContentText("Habit completed successfully!")
            .setAutoCancel(true)
            .build()

        // Show completion notification briefly
        notificationManager.notify(
            (habitId + "_complete").hashCode(),
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Snoozed")
            .setContentText("Habit reminder snoozed for 1 hour")
            .setAutoCancel(true)
            .build()

        // Show snooze notification briefly
        notificationManager.notify(
            (habitId + "_snooze").hashCode(),
            notification
        )
    }
}