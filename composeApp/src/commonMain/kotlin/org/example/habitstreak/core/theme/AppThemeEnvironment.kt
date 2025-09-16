package org.example.habitstreak.core.theme

import androidx.compose.runtime.*

/**
 * Global app theme state that can be modified at runtime
 */
var customAppTheme: AppTheme? by mutableStateOf(null)

/**
 * Expected object for platform-specific theme management
 */
expect object LocalAppTheme {
    val current: Boolean
    @Composable
    infix fun provides(theme: AppTheme?): ProvidedValue<*>
}

/**
 * Composable wrapper that provides theme environment
 */
@Composable
fun AppThemeEnvironment(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalAppTheme provides customAppTheme
    ) {
        // Key forces recomposition when theme changes
        key(customAppTheme) {
            content()
        }
    }
}

/**
 * Helper function to change app theme at runtime
 */
fun changeAppTheme(theme: AppTheme?) {
    println("🎨 AppThemeEnvironment.changeAppTheme: Changing from '${customAppTheme}' to '${theme}'")
    customAppTheme = theme
    println("✅ AppThemeEnvironment.changeAppTheme: customAppTheme is now '${customAppTheme}'")
}