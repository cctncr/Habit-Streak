package org.example.habitstreak.core.util

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual object SystemLocaleProvider {
    actual fun getSystemLocaleCode(): String {
        val systemLocale = NSLocale.currentLocale
        val languageCode = systemLocale.languageCode

        println("🌐 SystemLocaleProvider (iOS): Detected system locale: $languageCode")

        // Map system locale to supported locales
        return when (languageCode) {
            "tr" -> "tr"
            "en" -> "en"
            else -> {
                println("⚠️ SystemLocaleProvider (iOS): Unsupported locale '$languageCode', defaulting to 'en'")
                "en" // Default to English for unsupported locales
            }
        }
    }
}