package org.example.habitstreak.di

import org.example.habitstreak.data.local.DatabaseDriverFactory
import org.koin.dsl.module

/**
 * iOS specific dependencies
 */
val iosModule = module {
    single { DatabaseDriverFactory().createDriver() }
}