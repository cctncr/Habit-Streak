package org.example.habitstreak.platform

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime
import org.example.habitstreak.MainActivity
import org.example.habitstreak.domain.usecase.notification.CheckHabitActiveDayUseCase
import org.koin.core.context.GlobalContext

/**
 * AlarmReceiver for handling exact timing notifications from AlarmManager
 * Follows Single Responsibility Principle - only handles alarm-triggered notifications
 */
@OptIn(ExperimentalTime::class)
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_TITLE = "habit_title"
        const val EXTRA_MESSAGE = "message"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Time to complete your habit!"

        val koinContext = GlobalContext.getOrNull()
        if (koinContext == null) {
            // Fallback to basic notification if DI not available
            showNotification(context, habitId, habitTitle, message, isActive = true)
            return
        }

        // Check if habit is active today before showing notification
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val checkActiveDayUseCase = koinContext.get<CheckHabitActiveDayUseCase>()
                val dateProvider = koinContext.get<org.example.habitstreak.domain.util.DateProvider>()
                val today = dateProvider.today()

                val result = checkActiveDayUseCase(
                    CheckHabitActiveDayUseCase.Params(
                        habitId = habitId,
                        date = today
                    )
                )

                result.fold(
                    onSuccess = { isActive ->
                        showNotification(context, habitId, habitTitle, message, isActive)
                    },
                    onFailure = {
                        // Show notification with complete button on error (safe fallback)
                        showNotification(context, habitId, habitTitle, message, isActive = true)
                    }
                )
            } catch (e: Exception) {
                // Show notification with complete button on error (safe fallback)
                showNotification(context, habitId, habitTitle, message, isActive = true)
            }
        }
    }

    private fun showNotification(
        context: Context,
        habitId: String,
        habitTitle: String,
        message: String,
        isActive: Boolean
    ) {
        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        // Create intent to open app when notification is clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("habit_id", habitId)
            putExtra("navigate_to_habit", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        // Create notification with different content based on habit activity
        val notificationBuilder = NotificationCompat.Builder(
            context,
            AndroidNotificationScheduler.CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (isActive) {
            // Active day - show completion action
            notificationBuilder
                .setContentTitle("$habitTitle Reminder")
                .setContentText(message)
                .addAction(
                    android.R.drawable.ic_input_add,
                    "Mark Complete",
                    createCompleteActionIntent(context, habitId)
                )
        } else {
            // Inactive day - different message, no completion action
            notificationBuilder
                .setContentTitle("$habitTitle Rest Day")
                .setContentText("$habitTitle is not scheduled for today. Take a well-deserved break!")
        }

        val notification = notificationBuilder.build()

        // Show notification
        notificationManager.notify(habitId.hashCode(), notification)
    }

    private fun createCompleteActionIntent(context: Context, habitId: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "COMPLETE_HABIT"
            putExtra("habit_id", habitId)
        }

        return PendingIntent.getBroadcast(
            context,
            habitId.hashCode() + 1,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

}