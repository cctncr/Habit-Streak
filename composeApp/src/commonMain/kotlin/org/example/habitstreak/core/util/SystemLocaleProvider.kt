package org.example.habitstreak.core.util

/**
 * Platform-specific system locale detection
 */
expect object SystemLocaleProvider {
    /**
     * Get the current system locale code (e.g., "en", "tr")
     */
    fun getSystemLocaleCode(): String
}