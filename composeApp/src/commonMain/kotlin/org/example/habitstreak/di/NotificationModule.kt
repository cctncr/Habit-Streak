package org.example.habitstreak.di

import org.example.habitstreak.data.repository.NotificationRepositoryImpl
import org.example.habitstreak.domain.repository.NotificationRepository
import org.example.habitstreak.domain.service.NotificationService
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
}