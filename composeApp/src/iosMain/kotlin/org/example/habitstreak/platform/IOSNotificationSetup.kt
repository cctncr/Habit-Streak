package org.example.habitstreak.platform

import platform.UIKit.UIApplication
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNUserNotificationCenter

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
object IOSNotificationSetup {

    fun initialize() {
        val scheduler = IOSNotificationScheduler()
        scheduler.setupNotificationCategories()

        val delegate = IOSNotificationDelegate()
        UNUserNotificationCenter.currentNotificationCenter().delegate = delegate

        // Request permission on first launch
        // This should be called from your iOS app delegate
    }

    fun registerForRemoteNotifications() {
        UIApplication.sharedApplication.registerForRemoteNotifications()
    }
}