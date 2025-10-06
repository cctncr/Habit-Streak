package org.example.habitstreak.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlin.time.ExperimentalTime
import org.example.habitstreak.domain.usecase.notification.CheckHabitActiveDayUseCase
import org.example.habitstreak.domain.usecase.notification.NotificationPreferencesUseCase
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

    private suspend fun showNotification(habitId: String, habitTitle: String, message: String, isActive: Boolean) {
        // Get notification preferences
        val koinContext = GlobalContext.getOrNull()
        var soundEnabled = true
        var vibrationEnabled = true

        if (koinContext != null) {
            try {
                val prefsUseCase = koinContext.get<NotificationPreferencesUseCase>()
                val prefs = prefsUseCase.get()
                soundEnabled = prefs.soundEnabled
                vibrationEnabled = prefs.vibrationEnabled
            } catch (e: Exception) {
                // Use defaults if preferences can't be loaded
            }
        }

        // Use shared NotificationDisplayHelper for consistent notification display
        NotificationDisplayHelper.showHabitNotification(
            context = applicationContext,
            habitId = habitId,
            habitTitle = habitTitle,
            message = message,
            isActive = isActive,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled
        )
    }

}