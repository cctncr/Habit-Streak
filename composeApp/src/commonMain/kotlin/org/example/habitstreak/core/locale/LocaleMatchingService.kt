package org.example.habitstreak.core.locale

import org.example.habitstreak.core.locale.AppLocale

object LocaleMatchingService {

    /**
     * Find the best matching supported locale for the given system locale code
     */
    fun findBestMatch(systemLocaleCode: String): AppLocale {
        val normalizedCode = systemLocaleCode.lowercase()

        // First, try exact match
        val exactMatch = AppLocale.entries.find { it.code == normalizedCode }
        if (exactMatch != null) {
            return exactMatch
        }

        // Try language-only match (e.g., "tr-TR" -> "tr")
        val languageCode = normalizedCode.split("-", "_").first()
        val languageMatch = AppLocale.entries.find { it.code == languageCode }
        if (languageMatch != null) {
            return languageMatch
        }

        // Apply fallback chain based on language families and geographic proximity
        val fallbackMatch = when (languageCode) {
            // Turkish language family
            "az", "kk", "ky", "tk", "ug", "uz" -> AppLocale.TURKISH

            // Germanic languages (close to English)
            "de", "nl", "sv", "da", "no", "is" -> AppLocale.ENGLISH

            // Romance languages (default to English as most widely known second language)
            "es", "fr", "it", "pt", "ro", "ca" -> AppLocale.ENGLISH

            // Slavic languages (default to English)
            "ru", "uk", "pl", "cs", "sk", "bg", "hr", "sr", "sl" -> AppLocale.ENGLISH

            // Other European languages
            "fi", "hu", "et", "lv", "lt" -> AppLocale.ENGLISH

            // Middle Eastern languages (Turkish might be more familiar than English in some regions)
            "ar", "fa", "ku" -> AppLocale.TURKISH

            // Default fallback
            else -> AppLocale.ENGLISH
        }

        return fallbackMatch
    }

    /**
     * Get available locales in order of preference for the given system locale
     */
    fun getLocalePriorityOrder(systemLocaleCode: String): List<AppLocale> {
        val bestMatch = findBestMatch(systemLocaleCode)
        val allLocales = AppLocale.entries.toMutableList()

        // Put best match first, then others
        allLocales.remove(bestMatch)
        return listOf(bestMatch) + allLocales
    }
}