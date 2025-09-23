package org.example.habitstreak.core.locale

/**
 * Platform-specific system locale detection
 */
expect object SystemLocaleProvider {
    /**
     * Get the current system locale code (e.g., "en", "tr")
     */
    fun getSystemLocaleCode(): String
}