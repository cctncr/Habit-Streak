package org.example.habitstreak.core.theme

/**
 * Supported themes for the application
 */
enum class AppTheme(val code: String, val displayName: String) {
    SYSTEM("system", "System default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    PURPLE("purple", "Purple Night"),  // Custom theme 1
    GREEN("green", "Forest Green"),   // Custom theme 2
    BLUE("blue", "Ocean Blue");       // Custom theme 3

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

        /**
         * Get custom themes only (excluding system, light, dark)
         */
        fun getCustomThemes(): List<AppTheme> {
            return listOf(PURPLE, GREEN, BLUE)
        }
    }
}

/**
 * Theme mode that determines how the theme should be applied
 */
sealed class ThemeMode {
    object System : ThemeMode()
    object Light : ThemeMode()
    object Dark : ThemeMode()
    data class Custom(val theme: AppTheme) : ThemeMode()

    companion object {
        fun fromAppTheme(appTheme: AppTheme): ThemeMode {
            return when (appTheme) {
                AppTheme.SYSTEM -> System
                AppTheme.LIGHT -> Light
                AppTheme.DARK -> Dark
                else -> Custom(appTheme)
            }
        }
    }
}