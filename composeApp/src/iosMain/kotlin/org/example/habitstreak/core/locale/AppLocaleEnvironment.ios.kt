package org.example.habitstreak.core.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual object LocalAppLocale {
    private val defaultLocale: String get() = NSLocale.currentLocale.languageCode

    actual val current: String
        @Composable
        get() = defaultLocale

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        // For iOS, we don't need to modify system-wide AppleLanguages
        // The locale change is handled by our app-level state management
        // iOS resources will follow the locale through our resourceEnvironment

        // Return empty ProvidedValue since iOS locale is managed app-level
        return androidx.compose.runtime.compositionLocalOf { Unit } provides Unit
    }
}