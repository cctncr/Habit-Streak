package org.example.habitstreak.core.theme

/**
 * Platform-specific system theme detection
 */
expect object SystemThemeProvider {
    fun isSystemInDarkMode(): Boolean
}