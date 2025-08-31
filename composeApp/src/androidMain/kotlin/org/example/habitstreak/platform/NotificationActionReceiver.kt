package org.example.habitstreak.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.service.NotificationService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

class NotificationActionReceiver : BroadcastReceiver(), KoinComponent {

    private val habitRecordRepository: HabitRecordRepository by inject()
    private val notificationService: NotificationService by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habitId") ?: return

        when (intent.action) {
            "COMPLETE_HABIT" -> {
                handleCompleteHabit(context, habitId)
            }
            "SNOOZE_HABIT" -> {
                handleSnoozeHabit(context, habitId)
            }
        }

        // Cancel the notification
        NotificationManagerCompat.from(context).cancel(habitId.hashCode())
    }

    @OptIn(ExperimentalTime::class)
    private fun handleCompleteHabit(context: Context, habitId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            // Mark habit as complete for today
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            habitRecordRepository.markHabitAsComplete(
                habitId = habitId,
                date = today,
                count = 1,
                note = "Completed via notification"
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun handleSnoozeHabit(context: Context, habitId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            // Reschedule notification for 1 hour later
            val newTime = kotlin.time.Clock.System.now()
                .plus(1.hours) // Use Duration.Companion.hours
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .time

            notificationService.updateNotificationTime(habitId, newTime)
        }
    }
}