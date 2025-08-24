package org.example.habit_streak.di

import org.example.habit_streak.data.local.DatabaseDriverFactory
import org.koin.dsl.module

/**
 * Desktop specific dependencies
 */
val desktopModule = module {
    single { DatabaseDriverFactory().createDriver() }
}