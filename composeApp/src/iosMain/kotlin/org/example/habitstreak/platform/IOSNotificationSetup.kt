package org.example.habitstreak.platform

import platform.UIKit.UIApplication
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNUserNotificationCenter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.usecase.habit.ToggleHabitCompletionUseCase

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
object IOSNotificationSetup : KoinComponent {

    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return

        try {
            val habitRepository: HabitRepository by inject()
            val notificationService: NotificationService by inject()
            val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase by inject()
            val scheduler: IOSNotificationScheduler by inject()

            scheduler.setupNotificationCategories()

            val delegate = IOSNotificationDelegate(
                habitRepository = habitRepository,
                notificationService = notificationService,
                toggleHabitCompletionUseCase = toggleHabitCompletionUseCase
            )
            UNUserNotificationCenter.currentNotificationCenter().delegate = delegate

            isInitialized = true
        } catch (e: Exception) {
            println("IOSNotificationSetup initialization failed: ${e.message}")
            // Don't mark as initialized so we can retry later
        }
    }

    fun registerForRemoteNotifications() {
        UIApplication.sharedApplication.registerForRemoteNotifications()
    }
}