package org.example.habit_streak.di

import org.koin.dsl.module

val androidModule = module {
    // Room Database (will be added later)
    // single { Room.databaseBuilder(
    //     androidContext(),
    //     HabitDatabase::class.java,
    //     "habit_database"
    // ).build() }

    // single { get<HabitDatabase>().habitDao() }
    // single { get<HabitDatabase>().habitRecordDao() }
}