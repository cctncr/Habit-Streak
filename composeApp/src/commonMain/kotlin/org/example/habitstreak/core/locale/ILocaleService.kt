package org.example.habitstreak.core.locale

import org.example.habitstreak.core.locale.AppLocale

interface ILocaleService {
    suspend fun changeLocale(locale: AppLocale)
    suspend fun initializeLocale()
    fun getAvailableLocales(): List<AppLocale>
    fun getSystemLocale(): AppLocale
}