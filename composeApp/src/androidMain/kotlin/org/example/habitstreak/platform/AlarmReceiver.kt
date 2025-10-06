package org.example.habitstreak.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import org.example.habitstreak.domain.usecase.notification.CheckHabitActiveDayUseCase
import org.example.habitstreak.domain.usecase.notification.NotificationPreferencesUseCase
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

        println("🔔 ALARM_RECEIVER: Received alarm for habit: $habitTitle (ID: $habitId)")

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

                // ✅ FIX: Reschedule alarm for next occurrence (weekly repeat)
                rescheduleNotification(koinContext, habitId)
            } catch (e: Exception) {
                // Show notification with complete button on error (safe fallback)
                showNotification(context, habitId, habitTitle, message, isActive = true)

                // Still try to reschedule
                try {
                    rescheduleNotification(koinContext, habitId)
                } catch (rescheduleError: Exception) {
                    println("❌ ALARM_RECEIVER: Failed to reschedule: ${rescheduleError.message}")
                }
            }
        }
    }

    /**
     * Reschedule notification for next week occurrence
     * This is needed because we use setExact/setExactAndAllowWhileIdle (one-time alarms)
     * instead of setRepeating (inexact on modern Android)
     */
    private suspend fun rescheduleNotification(koinContext: org.koin.core.Koin, habitId: String) {
        try {
            val notificationRepo = koinContext.get<org.example.habitstreak.domain.repository.NotificationRepository>()
            val habitRepo = koinContext.get<org.example.habitstreak.domain.repository.HabitRepository>()
            val scheduler = koinContext.get<org.example.habitstreak.domain.service.NotificationScheduler>()

            val config = notificationRepo.getNotificationConfig(habitId)
            val habit = habitRepo.getHabitById(habitId).getOrNull()

            if (config != null && habit != null && config.isEnabled) {
                // Reschedule for next occurrence
                scheduler.scheduleNotification(config, habit.frequency, habit.createdAt)
                println("✅ ALARM_RECEIVER: Rescheduled notification for habit: $habitId")
            } else {
                println("⚠️ ALARM_RECEIVER: Cannot reschedule - config or habit not found/disabled")
            }
        } catch (e: Exception) {
            println("❌ ALARM_RECEIVER: Reschedule failed: ${e.message}")
        }
    }

    private fun showNotification(
        context: Context,
        habitId: String,
        habitTitle: String,
        message: String,
        isActive: Boolean
    ) {
        // Get notification preferences
        val koinContext = GlobalContext.getOrNull()
        var soundEnabled = true
        var vibrationEnabled = true

        if (koinContext != null) {
            try {
                val prefsUseCase = koinContext.get<NotificationPreferencesUseCase>()
                val prefs = kotlinx.coroutines.runBlocking { prefsUseCase.get() }
                soundEnabled = prefs.soundEnabled
                vibrationEnabled = prefs.vibrationEnabled
            } catch (e: Exception) {
                // Use defaults if preferences can't be loaded
            }
        }

        // Use shared NotificationDisplayHelper for consistent notification display
        CoroutineScope(Dispatchers.IO).launch {
            NotificationDisplayHelper.showHabitNotification(
                context = context,
                habitId = habitId,
                habitTitle = habitTitle,
                message = message,
                isActive = isActive,
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled
            )
        }
    }

}