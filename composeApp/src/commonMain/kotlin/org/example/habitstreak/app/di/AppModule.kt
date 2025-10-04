package org.example.habitstreak.app.di

import org.example.habitstreak.data.repository.HabitRecordRepositoryImpl
import org.example.habitstreak.data.repository.HabitRepositoryImpl
import org.example.habitstreak.data.repository.NotificationRepositoryImpl
import org.example.habitstreak.data.repository.createPreferencesRepository
import org.example.habitstreak.data.repository.StatisticsRepositoryImpl
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.repository.NotificationRepository
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.repository.StatisticsRepository
import org.example.habitstreak.domain.service.HabitValidationService
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.usecase.InitializeCategoriesUseCase
import org.example.habitstreak.domain.usecase.habit.CreateHabitUseCase
import org.example.habitstreak.domain.usecase.habit.ToggleHabitCompletionUseCase
import org.example.habitstreak.domain.usecase.habit.GetHabitsWithCompletionUseCase
import org.example.habitstreak.domain.usecase.habit.ArchiveHabitUseCase
import org.example.habitstreak.domain.usecase.habit.CalculateHabitStatsUseCase
import org.example.habitstreak.domain.usecase.notification.ManageHabitNotificationUseCase
import org.example.habitstreak.presentation.viewmodel.*
import org.koin.dsl.module
import org.example.habitstreak.data.local.HabitDatabase
import org.example.habitstreak.data.repository.CategoryRepositoryImpl
import org.example.habitstreak.domain.repository.CategoryRepository
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.domain.util.DateProviderImpl
import org.example.habitstreak.core.locale.*
import org.example.habitstreak.core.theme.*
import org.example.habitstreak.presentation.permission.PermissionFlowHandler
import org.example.habitstreak.data.cache.PermissionStateCache

/**
 * Main application module with improved SOLID compliance.
 * Modularized for better organization and maintainability.
 */
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

    // Repositories - Following Dependency Inversion Principle
    single<PreferencesRepository> { createPreferencesRepository() }
    single<HabitRepository> { HabitRepositoryImpl(get()) }
    single<HabitRecordRepository> { HabitRecordRepositoryImpl(get(), get()) }
    single<StatisticsRepository> { StatisticsRepositoryImpl(get(), get(), get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get()) }
    single<CategoryRepository> {
        CategoryRepositoryImpl(
            database = get(),
            dateProvider = get()
        )
    }

    // Domain Services - Business logic following SRP
    single { HabitValidationService() }
    single { org.example.habitstreak.domain.service.HabitFilterService() }

    // Permission Services - Following SRP for permission management
    single { PermissionStateCache() }
    single {
        PermissionFlowHandler(
            permissionManager = get(),
            stateCache = get()
        )
    }


    // Notification Services - NotificationScheduler from platform modules

    // Use Cases - Following Single Responsibility Principle
    factory { CreateHabitUseCase(get(), get(), get(), get()) }
    factory { ToggleHabitCompletionUseCase(get()) }
    factory { GetHabitsWithCompletionUseCase(get(), get()) }
    factory { org.example.habitstreak.domain.usecase.habit.CalculateStreakUseCase(get(), get(), get()) }
    factory { CalculateHabitStatsUseCase(get(), get()) }
    factory { ArchiveHabitUseCase(get()) }
    factory { InitializeCategoriesUseCase(get()) }
    factory { org.example.habitstreak.domain.usecase.notification.CompleteHabitFromNotificationUseCase(get()) }
    factory { org.example.habitstreak.domain.usecase.notification.CheckHabitActiveDayUseCase(get()) }

    // ViewModels - Following Dependency Injection best practices
    factory { HabitsViewModel(get(), get(), get(), get(), get(), get(), get()) }
    factory { (habitId: String?) ->
        CreateEditHabitViewModel(
            createHabitUseCase = get(),
            habitRepository = get(),
            categoryRepository = get(),
            preferencesRepository = get(),
            manageHabitNotificationUseCase = get(),
            updateNotificationPeriodUseCase = get(),
            habitId = habitId
        )
    }
    factory { StatisticsViewModel(get(), get()) }
    factory { (habitId: String) ->
        HabitDetailViewModel(
            habitId = habitId,
            habitRepository = get(),
            habitRecordRepository = get(),
            calculateHabitStatsUseCase = get(),
            manageHabitNotificationUseCase = get(),
            checkGlobalNotificationStatusUseCase = get(),
            enableGlobalNotificationsUseCase = get(),
            updateNotificationPreferencesUseCase = get(),
            getNotificationPreferencesUseCase = get(),
            updateNotificationPeriodUseCase = get(),
            preferencesRepository = get(),
            dateProvider = get()
        )
    }
    factory {
        SettingsViewModel(
            preferencesRepository = get(),
            localeService = get(),
            localeStateHolder = get(),
            themeService = get(),
            themeStateHolder = get(),
            permissionFlowHandler = get(),
            enableGlobalNotificationsUseCase = get(),
            disableGlobalNotificationsUseCase = get(),
            updateNotificationPreferencesUseCase = get()
        )
    }
}