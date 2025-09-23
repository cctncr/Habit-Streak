package org.example.habitstreak.di

import org.example.habitstreak.data.local.DatabaseDriverFactory
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.platform.AndroidNotificationScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual fun platformModule() = module {
    single { DatabaseDriverFactory(androidContext()) }
    single<NotificationScheduler> { AndroidNotificationScheduler(androidContext()) }
}