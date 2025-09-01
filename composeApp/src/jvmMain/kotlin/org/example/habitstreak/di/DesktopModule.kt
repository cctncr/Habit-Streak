package org.example.habitstreak.di

import org.example.habitstreak.data.local.DatabaseDriverFactory
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.domain.service.PermissionManager
import org.example.habitstreak.platform.DesktopNotificationScheduler
import org.example.habitstreak.platform.DesktopPermissionManager
import org.koin.dsl.module

/**
 * Desktop specific dependencies
 */
val desktopModule = module {
    single { DatabaseDriverFactory().createDriver() }
    single<NotificationScheduler> { DesktopNotificationScheduler() }
    single<PermissionManager> { DesktopPermissionManager() }
}