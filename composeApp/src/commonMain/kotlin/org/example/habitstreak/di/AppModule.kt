package org.example.habitstreak.di

import org.example.habitstreak.data.repository.HabitRecordRepositoryImpl
import org.example.habitstreak.data.repository.HabitRepositoryImpl
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.repository.StatisticsRepository
import org.example.habitstreak.domain.usecase.ArchiveHabitUseCase
import org.example.habitstreak.domain.usecase.CalculateStreakUseCase
import org.example.habitstreak.domain.usecase.CreateHabitUseCase
import org.example.habitstreak.domain.usecase.GetHabitsWithCompletionUseCase
import org.example.habitstreak.domain.usecase.ToggleHabitCompletionUseCase
import org.example.habitstreak.presentation.viewmodel.CreateEditHabitViewModel
import org.example.habitstreak.presentation.viewmodel.HabitsViewModel
import org.example.habitstreak.presentation.viewmodel.StatisticsViewModel
import org.koin.dsl.module
import org.example.habitstreak.data.local.HabitDatabase
import org.example.habitstreak.data.repository.StatisticsRepositoryImpl
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.domain.util.DateProviderImpl
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel

val appModule = module {
    // Core utilities
    single<DateProvider> { DateProviderImpl() }

    // Database
    single { HabitDatabase(get()) }

    // Repositories
    single<HabitRepository> { HabitRepositoryImpl(get()) }
    single<HabitRecordRepository> { HabitRecordRepositoryImpl(get(), get()) }
    single<StatisticsRepository> { StatisticsRepositoryImpl(get(), get(), get()) }

    // Use Cases
    factory { CreateHabitUseCase(get(), get()) }
    factory { ToggleHabitCompletionUseCase(get()) }
    factory { GetHabitsWithCompletionUseCase(get(), get()) }
    factory { CalculateStreakUseCase(get(), get(), get()) }
    factory { ArchiveHabitUseCase(get()) }

    // ViewModels
    factory { HabitsViewModel(get(), get(), get(), get(), get(), get()) }
    factory { (habitId: String?) -> CreateEditHabitViewModel(get(), get(), habitId) }
    factory { StatisticsViewModel(get(), get()) }
    factory { (habitId: String) ->
        HabitDetailViewModel(
            habitId = habitId,
            habitRepository = get(),
            habitRecordRepository = get(),
            calculateStreakUseCase = get(),
            dateProvider = get()
        )
    }
}