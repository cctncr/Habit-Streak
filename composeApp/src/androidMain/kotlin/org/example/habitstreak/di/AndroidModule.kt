package org.example.habitstreak.di

import org.example.habitstreak.data.local.DatabaseDriverFactory
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.domain.service.PermissionManager
import org.example.habitstreak.platform.AndroidNotificationScheduler
import org.example.habitstreak.platform.AndroidPermissionManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(androidContext()).createDriver() }
    single<NotificationScheduler> { AndroidNotificationScheduler(get()) }
    single<PermissionManager> { AndroidPermissionManager(get()) }
}