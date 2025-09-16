package org.example.habitstreak.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf

actual object LocalAppTheme {
    actual val current: Boolean
        @Composable
        get() = SystemThemeProvider.isSystemInDarkMode()

    @Composable
    actual infix fun provides(theme: AppTheme?): ProvidedValue<*> {
        // For iOS, theme changes are managed through our app-level state
        // Return empty ProvidedValue since iOS theme is managed app-level
        return compositionLocalOf { Unit } provides Unit
    }
}