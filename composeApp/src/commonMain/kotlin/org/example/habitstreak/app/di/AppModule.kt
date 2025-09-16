package org.example.habitstreak.app.di

import org.example.habitstreak.data.repository.HabitRecordRepositoryImpl
import org.example.habitstreak.data.repository.HabitRepositoryImpl
import org.example.habitstreak.data.repository.NotificationRepositoryImpl
import org.example.habitstreak.data.repository.StatisticsRepositoryImpl
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.repository.NotificationRepository
import org.example.habitstreak.domain.repository.StatisticsRepository
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.usecase.InitializeCategoriesUseCase
import org.example.habitstreak.domain.usecase.habit.CreateHabitUseCase
import org.example.habitstreak.domain.usecase.habit.ToggleHabitCompletionUseCase
import org.example.habitstreak.domain.usecase.habit.GetHabitsWithCompletionUseCase
import org.example.habitstreak.domain.usecase.habit.ArchiveHabitUseCase
import org.example.habitstreak.presentation.viewmodel.*
import org.koin.dsl.module
import org.example.habitstreak.data.local.HabitDatabase
import org.example.habitstreak.data.repository.CategoryRepositoryImpl
import org.example.habitstreak.domain.repository.CategoryRepository
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.core.util.DateProviderImpl
import org.example.habitstreak.core.locale.*
import org.example.habitstreak.core.theme.*

val appModule = module {
    // Core utilities
    single<DateProvider> { DateProviderImpl() }

    // Locale services
    single<ILocaleStateHolder> { LocaleStateHolder() }
    single<ILocaleRepository> { LocaleRepositoryImpl(get()) }
    single<ILocaleService> { LocaleService(get(), get()) }

    // Theme services
    single<IThemeStateHolder> { ThemeStateHolder() }
    single<IThemeRepository> { ThemeRepositoryImpl(get()) }
    single<IThemeService> { ThemeService(get(), get()) }

    // Database
    single { HabitDatabase(get()) }

    // Repositories - PreferencesRepository is provided by platform modules
    single<HabitRepository> { HabitRepositoryImpl(get()) }
    single<HabitRecordRepository> { HabitRecordRepositoryImpl(get(), get()) }
    single<StatisticsRepository> { StatisticsRepositoryImpl(get(), get(), get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get()) }

    // Services - NotificationScheduler and PreferencesRepository from platform modules
    single {
        NotificationService(
            notificationRepository = get(),
            habitRepository = get(),
            scheduler = get(),
            preferencesRepository = get(),
            permissionManager = get()
        )
    }

    // Use Cases
    factory { CreateHabitUseCase(get(), get(), get()) }
    factory { ToggleHabitCompletionUseCase(get()) }
    factory { GetHabitsWithCompletionUseCase(get(), get()) }
    factory { org.example.habitstreak.domain.usecase.habit.CalculateStreakUseCase(get(), get(), get()) }
    factory { ArchiveHabitUseCase(get()) }
    factory { InitializeCategoriesUseCase(get()) }

    // ViewModels
    factory { HabitsViewModel(get(), get(), get(), get(), get(), get(), get()) }
    factory { (habitId: String?) -> CreateEditHabitViewModel(get(), get(), get(), habitId) }
    factory { StatisticsViewModel(get(), get()) }
    factory { (habitId: String) ->
        HabitDetailViewModel(
            habitId = habitId,
            habitRepository = get(),
            habitRecordRepository = get(),
            calculateStreakUseCase = get(),
            dateProvider = get(),
            notificationService = getOrNull(),
            preferencesRepository = get()
        )
    }
    factory {
        SettingsViewModel(
            preferencesRepository = get(),
            notificationService = getOrNull(),
            habitRepository = get(),
            localeService = get(),
            localeStateHolder = get(),
            themeService = get(),
            themeStateHolder = get()
        )
    }
    single<CategoryRepository> {
        CategoryRepositoryImpl(
            database = get(),
            dateProvider = get()
        )
    }
}