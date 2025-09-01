// composeApp/src/androidMain/kotlin/org/example/habitstreak/di/AndroidModule.kt
package org.example.habitstreak.di

import android.content.Context
import org.example.habitstreak.data.local.DatabaseDriverFactory
import org.example.habitstreak.data.repository.PreferencesRepositoryImpl
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.domain.service.PermissionManager
import org.example.habitstreak.platform.AndroidNotificationScheduler
import org.example.habitstreak.platform.AndroidPermissionManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android specific dependencies
 * Following Dependency Inversion Principle - providing implementations for interfaces
 */
val androidModule = module {
    // Database driver
    single { DatabaseDriverFactory(androidContext()).createDriver() }

    // Preferences repository
    single<PreferencesRepository> {
        PreferencesRepositoryImpl(androidContext())
    }

    // Notification scheduler - only handles scheduling
    single<NotificationScheduler> {
        AndroidNotificationScheduler(androidContext())
    }

    // Permission manager - handles all permission-related operations
    single<PermissionManager> {
        AndroidPermissionManager(androidContext())
    }
}