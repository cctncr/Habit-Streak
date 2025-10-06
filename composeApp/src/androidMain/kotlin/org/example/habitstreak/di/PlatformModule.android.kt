package org.example.habitstreak.app.di

import android.app.Application
import org.example.habitstreak.data.local.DatabaseDriverFactory
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.domain.service.PermissionManager
import org.example.habitstreak.platform.ActivityProvider
import org.example.habitstreak.platform.ActivityProviderImpl
import org.example.habitstreak.platform.AndroidNotificationScheduler
import org.example.habitstreak.platform.AndroidPermissionManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual fun platformModule() = module {
    single { DatabaseDriverFactory(androidContext()).createDriver() }
    single<ActivityProvider> { ActivityProviderImpl(androidApplication()) }
    single<NotificationScheduler> {
        AndroidNotificationScheduler(
            context = androidContext(),
            periodValidator = get(),
            preferencesRepository = get()
        )
    }
    single<PermissionManager> { AndroidPermissionManager(androidContext(), get()) }
}