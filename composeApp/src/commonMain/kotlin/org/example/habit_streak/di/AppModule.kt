package org.example.habit_streak.di

import org.example.habit_streak.domain.usecase.ArchiveHabitUseCase
import org.example.habit_streak.domain.usecase.CalculateStreakUseCase
import org.example.habit_streak.domain.usecase.CreateHabitUseCase
import org.example.habit_streak.domain.usecase.GetHabitsWithCompletionUseCase
import org.example.habit_streak.domain.usecase.ToggleHabitCompletionUseCase
import org.example.habit_streak.presentation.viewmodel.CreateEditHabitViewModel
import org.example.habit_streak.presentation.viewmodel.HabitsViewModel
import org.example.habit_streak.presentation.viewmodel.StatisticsViewModel
import org.koin.dsl.module

val appModule = module {
    // Repositories (will be implemented when Room is added)
    // single<HabitRepository> { HabitRepositoryImpl(get()) }
    // single<HabitRecordRepository> { HabitRecordRepositoryImpl(get()) }
    // single<StatisticsRepository> { StatisticsRepositoryImpl(get(), get()) }

    // Use Cases
    factory { CreateHabitUseCase(get()) }
    factory { ToggleHabitCompletionUseCase(get()) }
    factory { GetHabitsWithCompletionUseCase(get(), get()) }
    factory { CalculateStreakUseCase(get()) }
    factory { ArchiveHabitUseCase(get()) }

    // ViewModels
    factory { HabitsViewModel(get(), get(), get(), get()) }
    factory { (habitId: String?) -> CreateEditHabitViewModel(get(), get(), habitId) }
    factory { StatisticsViewModel(get(), get()) }
}