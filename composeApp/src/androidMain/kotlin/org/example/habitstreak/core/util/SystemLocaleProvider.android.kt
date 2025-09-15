package org.example.habitstreak.core.util

import java.util.Locale

actual object SystemLocaleProvider {
    actual fun getSystemLocaleCode(): String {
        val systemLocale = Locale.getDefault()
        val languageCode = systemLocale.language

        println("🌐 SystemLocaleProvider (Android): Detected system locale: $languageCode")

        // Map system locale to supported locales
        return when (languageCode) {
            "tr" -> "tr"
            "en" -> "en"
            else -> {
                println("⚠️ SystemLocaleProvider (Android): Unsupported locale '$languageCode', defaulting to 'en'")
                "en" // Default to English for unsupported locales
            }
        }
    }
}