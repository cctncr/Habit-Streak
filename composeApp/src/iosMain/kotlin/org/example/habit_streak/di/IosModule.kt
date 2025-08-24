package org.example.habit_streak.di

import org.example.habit_streak.data.local.DatabaseDriverFactory
import org.koin.dsl.module

/**
 * iOS specific dependencies
 */
val iosModule = module {
    single { DatabaseDriverFactory().createDriver() }
}