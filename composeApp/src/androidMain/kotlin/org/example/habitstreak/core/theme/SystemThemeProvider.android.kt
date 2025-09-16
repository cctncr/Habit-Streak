package org.example.habitstreak.core.theme

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration

actual object SystemThemeProvider {
    actual fun isSystemInDarkMode(): Boolean {
        return false // This will be overridden in the actual usage with LocalConfiguration
    }
}

/**
 * Composable function to check if system is in dark mode (Android)
 */
@androidx.compose.runtime.Composable
fun isSystemInDarkModeAndroid(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}