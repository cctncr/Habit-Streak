package org.example.habitstreak.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.UserNotifications.*
import platform.darwin.NSObject
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.usecase.habit.ToggleHabitCompletionUseCase
import org.example.habitstreak.domain.service.NotificationService
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalForeignApi::class)
class IOSNotificationDelegate(
    private val habitRepository: HabitRepository,
    private val notificationService: NotificationService,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase
) : NSObject(), UNUserNotificationCenterDelegateProtocol {

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit
    ) {
        val userInfo = didReceiveNotificationResponse.notification.request.content.userInfo
        val habitId = userInfo["habitId"] as? String ?: return

        when (didReceiveNotificationResponse.actionIdentifier) {
            "COMPLETE_ACTION" -> {
                handleCompleteHabit(habitId)
            }
            "SNOOZE_ACTION" -> {
                handleSnoozeHabit(habitId)
            }
            UNNotificationDefaultActionIdentifier -> {
                // User tapped on notification
                // Open the app to the specific habit
            }
        }

        withCompletionHandler()
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) {
        // Show notification even when app is in foreground
        withCompletionHandler(
            UNNotificationPresentationOptionAlert or
                    UNNotificationPresentationOptionBadge or
                    UNNotificationPresentationOptionSound
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun handleCompleteHabit(habitId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val params = ToggleHabitCompletionUseCase.Params(habitId, today, 1)
            toggleHabitCompletionUseCase(params)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun handleSnoozeHabit(habitId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val newTime = kotlin.time.Clock.System.now()
                .plus(1.hours) // Use Duration.Companion.hours
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .time

            notificationService.updateNotificationTime(habitId, newTime)
        }
    }
}