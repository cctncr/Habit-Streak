package org.example.habitstreak.core.theme

actual object SystemThemeProvider {
    actual fun isSystemInDarkMode(): Boolean {
        // For JVM/Desktop, we can check system properties or default to false
        return try {
            System.getProperty("os.name", "").lowercase().contains("windows") &&
                    System.getProperty("user.name", "").isNotEmpty()
            // Simple check - you can enhance this with more sophisticated detection
            false
        } catch (e: Exception) {
            false
        }
    }
}