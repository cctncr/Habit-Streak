package org.example.habitstreak.core.locale

import org.example.habitstreak.core.util.AppLocale

interface ILocaleService {
    suspend fun changeLocale(locale: AppLocale)
    suspend fun initializeLocale()
    fun getAvailableLocales(): List<AppLocale>
    fun getSystemLocale(): AppLocale
}