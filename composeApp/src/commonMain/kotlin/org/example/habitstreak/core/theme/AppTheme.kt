package org.example.habitstreak.core.theme

/**
 * Supported themes for the application
 */
enum class AppTheme(val code: String, val displayName: String) {
    SYSTEM("system", "System default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    PURPLE("purple", "Purple Night"),
    GREEN("green", "Forest Green"),
    BLUE("blue", "Ocean Blue");

    companion object {
        fun fromCode(code: String): AppTheme {
            return entries.find { it.code == code } ?: SYSTEM
        }

        /**
         * Get all available themes
         */
        fun getAvailableThemes(): List<AppTheme> {
            return entries.toList()
        }
    }
}

/**
 * Theme mode that determines how the theme should be applied
 */
sealed class ThemeMode {
    data object System : ThemeMode()
    data class Custom(val theme: AppTheme) : ThemeMode()
}