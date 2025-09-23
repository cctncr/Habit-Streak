package org.example.habitstreak.data.repository

import org.example.habitstreak.domain.repository.PreferencesRepository

/**
 * Platform-specific factory for PreferencesRepository implementations.
 *
 * This follows the Dependency Inversion Principle (DIP) by providing a common factory
 * interface that can be implemented differently on each platform while maintaining
 * the same contract. Platform modules will provide their own implementation in AppModule.
 *
 * Platform implementations:
 * - Android: Uses DataStore for persistent storage
 * - iOS: Uses NSUserDefaults for persistent storage
 * - JVM: Uses Java Preferences API for persistent storage
 */
expect fun createPreferencesRepository(): PreferencesRepository