package org.example.habitstreak.core.locale

import kotlinx.coroutines.flow.first
import org.example.habitstreak.core.util.AppLocale
import org.example.habitstreak.core.util.SystemLocaleProvider

class LocaleService(
    private val localeRepository: ILocaleRepository,
    private val localeStateHolder: ILocaleStateHolder
) : ILocaleService {

    override suspend fun changeLocale(locale: AppLocale) {
        println("🔧 LocaleService.changeLocale: Changing to ${locale.code}")
        localeStateHolder.setCurrentLocale(locale)
        localeRepository.setLocale(locale.code)
        // Update the app-wide locale environment for stringResource()
        changeAppLocale(locale.code)
        println("✅ LocaleService.changeLocale: Completed change to ${locale.code}")
    }

    override suspend fun initializeLocale() {
        val savedLocaleCode = localeRepository.getLocale()
        val savedLocale = AppLocale.fromCode(savedLocaleCode)

        // Always use the saved locale (which includes system locale from first launch)
        localeStateHolder.setCurrentLocale(savedLocale)
        changeAppLocale(savedLocale.code)

        // Debug logging to check what's happening
        println("🔧 LocaleService.initializeLocale: savedLocaleCode='$savedLocaleCode', using locale='${savedLocale.code}'")
    }

    override fun getAvailableLocales(): List<AppLocale> {
        return AppLocale.values().toList()
    }

    override fun getSystemLocale(): AppLocale {
        val systemLocaleCode = SystemLocaleProvider.getSystemLocaleCode()
        return LocaleMatchingService.findBestMatch(systemLocaleCode)
    }
}