package org.example.habitstreak.core.locale

import androidx.compose.runtime.*


/**
 * Global app locale state that can be modified at runtime
 */
var customAppLocale: String? by mutableStateOf(null)

/**
 * Expected object for platform-specific locale management
 */
expect object LocalAppLocale {
    val current: String
    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

/**
 * Composable wrapper that provides locale environment for string resources
 */
@Composable
fun AppEnvironment(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppLocale provides customAppLocale
    ) {
        // Key forces recomposition when locale changes
        key(customAppLocale) {
            content()
        }
    }
}

/**
 * Helper function to change app locale at runtime
 */
fun changeAppLocale(localeCode: String?) {
    customAppLocale = localeCode
}