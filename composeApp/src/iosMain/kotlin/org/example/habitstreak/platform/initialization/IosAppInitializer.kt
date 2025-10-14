package org.example.habitstreak.platform.initialization

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.example.habitstreak.domain.initialization.AppInitializer
import org.example.habitstreak.domain.repository.HabitRepository

/**
 * iOS implementation of AppInitializer.
 *
 * This class follows SOLID principles:
 * - Single Responsibility: Handles iOS-specific initialization
 * - Liskov Substitution: Can be used anywhere AppInitializer is expected
 * - Dependency Inversion: Depends on AppInitializer abstraction
 *
 * iOS automatically displays the LaunchScreen.storyboard while the app is loading.
 * This implementation ensures that essential data (Habits) is loaded from
 * the database before the app UI is displayed to the user.
 */
class IosAppInitializer : AppInitializer {
    private var initialized = false

    /**
     * Perform iOS-specific initialization tasks.
     *
     * Waits for the first emission from HabitRepository to ensure that:
     * - Database is warmed up
     * - Habits are loaded (even if empty list)
     * - UI can safely display HabitCards
     *
     * @param habitRepository Repository to observe habit loading
     */
    override suspend fun initialize(habitRepository: HabitRepository) {
        if (initialized) return

        try {
            // Wait for habits to be loaded from database
            // This ensures HabitCards can be displayed immediately when splash screen ends
            // Using withTimeout to prevent indefinite waiting in case of database issues
            withTimeout(5000) { // 5 second timeout
                habitRepository.observeActiveHabitsWithCategories().first()
            }

            initialized = true
        } catch (e: Exception) {
            // Even if there's an error, mark as initialized to prevent app from being stuck
            // The app will show empty state or error state in the UI
            initialized = true
            throw e
        }
    }

    override suspend fun isAppReady(): Boolean {
        return initialized
    }
}
