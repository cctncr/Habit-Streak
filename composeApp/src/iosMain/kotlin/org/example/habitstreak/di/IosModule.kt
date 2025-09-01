package org.example.habitstreak.di

import org.example.habitstreak.data.local.DatabaseDriverFactory
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.domain.service.PermissionManager
import org.example.habitstreak.platform.IOSNotificationScheduler
import org.example.habitstreak.platform.IOSPermissionManager
import org.koin.dsl.module

/**
 * iOS specific dependencies
 */
val iosModule = module {
    single { DatabaseDriverFactory().createDriver() }
    single<NotificationScheduler> { IOSNotificationScheduler() }
    single<PermissionManager> { IOSPermissionManager() }
}