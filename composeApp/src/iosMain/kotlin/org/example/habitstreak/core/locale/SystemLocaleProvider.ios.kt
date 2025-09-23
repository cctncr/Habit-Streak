package org.example.habitstreak.core.locale

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual object SystemLocaleProvider {
    actual fun getSystemLocaleCode(): String {
        val systemLocale = NSLocale.currentLocale
        return systemLocale.languageCode
    }
}