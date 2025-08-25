package org.example.habitstreak.di

import org.example.habitstreak.data.local.DatabaseDriverFactory
import org.koin.dsl.module

/**
 * Desktop specific dependencies
 */
val desktopModule = module {
    single { DatabaseDriverFactory().createDriver() }
}