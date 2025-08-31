package org.example.habitstreak.di

import org.example.habitstreak.data.local.DatabaseDriverFactory
import org.example.habitstreak.data.repository.PreferencesRepositoryImpl
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.platform.IOSNotificationScheduler
import org.koin.dsl.module

actual fun platformModule() = module {
    single { DatabaseDriverFactory() }
    single<NotificationScheduler> { IOSNotificationScheduler() }
    single<PreferencesRepository> { PreferencesRepositoryImpl() }
}