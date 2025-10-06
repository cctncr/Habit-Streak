package org.example.habitstreak.core.locale

interface ILocaleService {
    suspend fun changeLocale(locale: AppLocale)
    suspend fun initializeLocale()
    fun getAvailableLocales(): List<AppLocale>
    fun getSystemLocale(): AppLocale
}