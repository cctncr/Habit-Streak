package org.example.habitstreak.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

actual object LocalAppTheme {
    actual val current: Boolean
        @Composable
        get() = isSystemInDarkModeAndroid()

    @Composable
    actual infix fun provides(theme: AppTheme?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current

        // For Android, we determine dark mode based on theme
        val shouldBeDarkMode = when (theme) {
            AppTheme.SYSTEM -> isSystemInDarkModeAndroid()
            AppTheme.DARK, AppTheme.PURPLE -> true
            AppTheme.LIGHT, AppTheme.GREEN, AppTheme.BLUE -> false
            null -> isSystemInDarkModeAndroid()
        }

        // Create new configuration if needed
        val newConfiguration = if (shouldBeDarkMode != isSystemInDarkModeAndroid()) {
            Configuration(configuration).apply {
                uiMode = if (shouldBeDarkMode) {
                    uiMode or Configuration.UI_MODE_NIGHT_YES
                } else {
                    uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or Configuration.UI_MODE_NIGHT_NO
                }
            }
        } else {
            configuration
        }

        return androidx.compose.ui.platform.LocalConfiguration provides newConfiguration
    }
}