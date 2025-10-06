package org.example.habitstreak.app.di

import org.example.habitstreak.data.local.DatabaseDriverFactory
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.domain.service.PermissionManager
import org.example.habitstreak.platform.IOSNotificationScheduler
import org.example.habitstreak.platform.IOSPermissionManager
import org.koin.dsl.module

actual fun platformModule() = module {
    single { DatabaseDriverFactory().createDriver() }
    single<NotificationScheduler> {
        IOSNotificationScheduler(
            preferencesRepository = get(),
            periodValidator = get()
        )
    }
    single<PermissionManager> { IOSPermissionManager() }
}