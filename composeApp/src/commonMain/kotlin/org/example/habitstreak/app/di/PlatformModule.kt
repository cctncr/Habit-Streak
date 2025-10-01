package org.example.habitstreak.app.di

import org.koin.core.module.Module

/**
 * Platform-specific DI module declaration
 * Each platform provides its own implementation (expect/actual pattern)
 */
expect fun platformModule(): Module
