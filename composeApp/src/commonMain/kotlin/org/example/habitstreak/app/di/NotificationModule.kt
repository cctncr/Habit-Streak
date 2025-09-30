package org.example.habitstreak.app.di

import org.example.habitstreak.data.repository.NotificationRepositoryImpl
import org.example.habitstreak.domain.repository.NotificationRepository
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.service.NotificationPermissionService
import org.example.habitstreak.domain.usecase.notification.ManageHabitNotificationUseCase
import org.example.habitstreak.domain.usecase.notification.CheckGlobalNotificationStatusUseCase
import org.example.habitstreak.domain.usecase.notification.EnableGlobalNotificationsUseCase
import org.example.habitstreak.domain.usecase.notification.DisableGlobalNotificationsUseCase
import org.koin.dsl.module

val notificationModule = module {
    single<NotificationRepository> {
        NotificationRepositoryImpl(get())
    }

    single {
        NotificationService(
            notificationRepository = get(),
            habitRepository = get(),
            scheduler = get(),
            preferencesRepository = get()
        )
    }

    single {
        NotificationPermissionService(
            permissionManager = get()
        )
    }

    single {
        ManageHabitNotificationUseCase(
            notificationService = get(),
            permissionService = get()
        )
    }

    single {
        CheckGlobalNotificationStatusUseCase(
            permissionManager = get(),
            preferencesRepository = get()
        )
    }

    single {
        EnableGlobalNotificationsUseCase(
            preferencesRepository = get(),
            notificationService = get(),
            habitRepository = get()
        )
    }

    single {
        DisableGlobalNotificationsUseCase(
            preferencesRepository = get(),
            notificationService = get(),
            habitRepository = get()
        )
    }
}