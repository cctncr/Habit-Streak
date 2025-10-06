package org.example.habitstreak.platform

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.example.habitstreak.MainActivity
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Shared utility for displaying notifications
 * Used by both AlarmReceiver and NotificationWorker
 * Follows DRY principle - single source of truth for notification display logic
 */
object NotificationDisplayHelper {

    /**
     * Show habit notification with localized strings
     * @param context Android context
     * @param habitId Unique habit identifier
     * @param habitTitle Habit title for display
     * @param message Notification message content
     * @param isActive Whether habit is active today (shows complete button if true)
     * @param soundEnabled Whether notification sound should play
     * @param vibrationEnabled Whether notification should vibrate
     */
    suspend fun showHabitNotification(
        context: Context,
        habitId: String,
        habitTitle: String,
        message: String,
        isActive: Boolean,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = createContentIntent(context, habitId)
        val builder = createNotificationBuilder(
            context = context,
            habitId = habitId,
            habitTitle = habitTitle,
            message = message,
            isActive = isActive,
            contentIntent = contentIntent,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled
        )

        notificationManager.notify(habitId.hashCode(), builder.build())
    }

    /**
     * Create PendingIntent for notification tap action (opens habit detail)
     */
    private fun createContentIntent(context: Context, habitId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("habit_id", habitId)
            putExtra("navigate_to_habit", true)
        }

        return PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

    /**
     * Create notification builder with localized content
     */
    private suspend fun createNotificationBuilder(
        context: Context,
        habitId: String,
        habitTitle: String,
        message: String,
        isActive: Boolean,
        contentIntent: PendingIntent,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, AndroidNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Changed from HIGH to DEFAULT to match IMPORTANCE_LOW channel
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(0) // Disable all defaults, we'll set them manually

        // ✅ FIX: Use setSilent() for proper sound control
        // setSilent(true) overrides channel importance and ensures no sound/vibration
        if (!soundEnabled) {
            builder.setSilent(true)
        } else {
            // Sound enabled - set sound and optional vibration
            builder.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)

            if (vibrationEnabled) {
                builder.setVibrate(longArrayOf(0, 250, 250, 250))
            }
        }

        // Set content based on habit activity status
        if (isActive) {
            // Active day - show completion action with localized strings
            val reminderTitle = getString(Res.string.notification_reminder_title).replace("%s", habitTitle)
            val markCompleteAction = getString(Res.string.notification_mark_complete_action)

            builder
                .setContentTitle(reminderTitle)
                .setContentText(message)
                .addAction(
                    android.R.drawable.ic_input_add,
                    markCompleteAction,
                    createCompleteActionIntent(context, habitId)
                )
        } else {
            // Inactive day - different message with localized strings, no completion action
            val restDayTitle = getString(Res.string.notification_rest_day_title).replace("%s", habitTitle)
            val restDayMessage = getString(Res.string.notification_rest_day_message).replace("%s", habitTitle)

            builder
                .setContentTitle(restDayTitle)
                .setContentText(restDayMessage)
        }

        return builder
    }

    /**
     * Create PendingIntent for "Mark Complete" action button
     */
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
