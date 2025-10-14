package org.example.habitstreak.domain.initialization

import org.example.habitstreak.domain.repository.HabitRepository

/**
 * Interface for platform-specific app initialization logic.
 *
 * This interface follows SOLID principles:
 * - Single Responsibility: Only handles app initialization state
 * - Interface Segregation: Minimal, focused contract
 * - Dependency Inversion: High-level modules depend on this abstraction
 *
 * The implementation must ensure that critical data (like Habits) is loaded
 * before the app UI is displayed.
 */
interface AppInitializer {
    /**
     * Perform initialization tasks required before showing the main UI.
     * This includes waiting for critical data (Habits) to be loaded from the database.
     *
     * @param habitRepository Repository to check if habits are loaded
     */
    suspend fun initialize(habitRepository: HabitRepository)

    /**
     * Check if the app is ready to be displayed to the user.
     *
     * @return true if app is ready, false otherwise
     */
    suspend fun isAppReady(): Boolean
}
