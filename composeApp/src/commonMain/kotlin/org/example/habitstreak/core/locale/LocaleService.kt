package org.example.habitstreak.core.locale

import kotlinx.coroutines.flow.first
import org.example.habitstreak.core.util.AppLocale
import org.example.habitstreak.core.util.SystemLocaleProvider

class LocaleService(
    private val localeRepository: ILocaleRepository,
    private val localeStateHolder: ILocaleStateHolder
) : ILocaleService {

    override suspend fun changeLocale(locale: AppLocale) {
        localeStateHolder.setCurrentLocale(locale)
        localeRepository.setLocale(locale.code)
    }

    override suspend fun initializeLocale() {
        val savedLocaleCode = localeRepository.getLocale()

        // Detect if this is first launch (no saved locale preference)
        val isFirstLaunch = savedLocaleCode.isEmpty() || savedLocaleCode == AppLocale.ENGLISH.code

        if (isFirstLaunch) {
            val systemLocale = getSystemLocale()
            localeStateHolder.setCurrentLocale(systemLocale)
            localeRepository.setLocale(systemLocale.code)
        } else {
            val savedLocale = AppLocale.fromCode(savedLocaleCode)
            localeStateHolder.setCurrentLocale(savedLocale)
        }
    }

    override fun getAvailableLocales(): List<AppLocale> {
        return AppLocale.values().toList()
    }

    override fun getSystemLocale(): AppLocale {
        val systemLocaleCode = SystemLocaleProvider.getSystemLocaleCode()
        return LocaleMatchingService.findBestMatch(systemLocaleCode)
    }
}