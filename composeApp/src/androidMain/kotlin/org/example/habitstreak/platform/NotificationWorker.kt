package org.example.habitstreak.platform

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime
import org.example.habitstreak.MainActivity
import org.example.habitstreak.domain.usecase.notification.CheckHabitActiveDayUseCase
import org.koin.core.context.GlobalContext

/**
 * Worker class for showing notifications
 * Follows Single Responsibility - only shows notifications
 */
@OptIn(ExperimentalTime::class)
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getString(AndroidNotificationScheduler.KEY_HABIT_ID)
            ?: return Result.failure()

        val habitTitle = inputData.getString(AndroidNotificationScheduler.KEY_HABIT_TITLE)
            ?: getHabitTitleFromDatabase(habitId)

        val message = inputData.getString(AndroidNotificationScheduler.KEY_MESSAGE)
            ?: "Time to complete your habit!"

        val koinContext = GlobalContext.getOrNull()
        if (koinContext == null) {
            // Fallback to basic notification if DI not available
            showNotification(habitId, habitTitle, message, isActive = true)
            return Result.success()
        }

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
                    showNotification(habitId, habitTitle, message, isActive)
                },
                onFailure = {
                    // Show notification with complete button on error (safe fallback)
                    showNotification(habitId, habitTitle, message, isActive = true)
                }
            )
        } catch (e: Exception) {
            // Show notification with complete button on error (safe fallback)
            showNotification(habitId, habitTitle, message, isActive = true)
        }

        return Result.success()
    }

    private suspend fun getHabitTitleFromDatabase(habitId: String): String {
        return try {
            val koinContext = GlobalContext.getOrNull()
            if (koinContext != null) {
                val habitRepository = koinContext.get<org.example.habitstreak.domain.repository.HabitRepository>()
                val result = habitRepository.getHabitById(habitId)
                result.getOrNull()?.title ?: "Habit"
            } else {
                "Habit"
            }
        } catch (e: Exception) {
            "Habit"
        }
    }

    private fun showNotification(habitId: String, habitTitle: String, message: String, isActive: Boolean) {
        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        // Create intent to open app when notification is clicked
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("habit_id", habitId)
            putExtra("navigate_to_habit", true)
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

        // Create notification with different content based on habit activity
        val notificationBuilder = NotificationCompat.Builder(
            applicationContext,
            AndroidNotificationScheduler.CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isActive) {
            // Active day - show completion action
            notificationBuilder
                .setContentTitle("$habitTitle Reminder")
                .setContentText(message)
                .addAction(
                    android.R.drawable.ic_input_add,
                    "Mark Complete",
                    createCompleteActionIntent(habitId)
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

}