package org.example.habitstreak.app.di

import org.example.habitstreak.data.repository.NotificationRepositoryImpl
import org.example.habitstreak.domain.repository.NotificationRepository
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.service.NotificationPeriodValidator
import org.example.habitstreak.domain.usecase.notification.HabitNotificationUseCase
import org.example.habitstreak.domain.usecase.notification.GlobalNotificationUseCase
import org.example.habitstreak.domain.usecase.notification.NotificationPreferencesUseCase
import org.example.habitstreak.domain.usecase.notification.CheckHabitActiveDayUseCase
import org.koin.dsl.module

val notificationModule = module {
    // Repository
    single<NotificationRepository> {
        NotificationRepositoryImpl(get())
    }

    // Service
    single {
        NotificationService(
            notificationRepository = get(),
            habitRepository = get(),
            scheduler = get(),
            preferencesRepository = get()
        )
    }

    // Validator
    single {
        NotificationPeriodValidator()
    }

    // Use Cases (SIMPLIFIED from 8 to 4)
    single {
        HabitNotificationUseCase(
            notificationService = get()
        )
    }

    single {
        GlobalNotificationUseCase(
            permissionManager = get(),
            preferencesRepository = get(),
            notificationService = get(),
            habitRepository = get(),
            notificationRepository = get(),
            scheduler = get()
        )
    }

    single {
        NotificationPreferencesUseCase(
            preferencesRepository = get(),
            notificationService = get()
        )
    }

    single {
        CheckHabitActiveDayUseCase(
            habitRepository = get()
        )
    }
}