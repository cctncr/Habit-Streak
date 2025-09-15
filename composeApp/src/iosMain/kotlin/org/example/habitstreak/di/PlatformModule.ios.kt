package org.example.habitstreak.di

import org.example.habitstreak.data.local.DatabaseDriverFactory
import org.example.habitstreak.data.repository.PreferencesRepositoryImpl
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.NotificationScheduler
import org.example.habitstreak.domain.service.PermissionManager
import org.example.habitstreak.platform.IOSNotificationScheduler
import org.example.habitstreak.platform.IOSPermissionManager
import org.example.habitstreak.domain.model.NotificationConfig
import org.koin.dsl.module
import app.cash.sqldelight.db.SqlDriver

actual fun platformModule() = module {
    single { DatabaseDriverFactory() }
    single<SqlDriver> { get<DatabaseDriverFactory>().createDriver() }
    // Temporarily disable NotificationScheduler to debug crash - use mock instead
    single<NotificationScheduler> {
        object : NotificationScheduler {
            override suspend fun scheduleNotification(config: NotificationConfig): Result<Unit> {
                return Result.success(Unit)
            }
            override suspend fun updateNotification(config: NotificationConfig): Result<Unit> {
                return Result.success(Unit)
            }
            override suspend fun cancelNotification(habitId: String): Result<Unit> {
                return Result.success(Unit)
            }
            override suspend fun cancelAllNotifications(): Result<Unit> {
                return Result.success(Unit)
            }
            override suspend fun isNotificationScheduled(habitId: String): Boolean {
                return false
            }
        }
    }
    single<PermissionManager> { IOSPermissionManager() }
    single<PreferencesRepository> { PreferencesRepositoryImpl() }
}