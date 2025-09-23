package org.example.habitstreak.core.locale

import java.util.Locale

actual object SystemLocaleProvider {
    actual fun getSystemLocaleCode(): String {
        val systemLocale = Locale.getDefault()
        val languageCode = systemLocale.language
        return languageCode
    }
}