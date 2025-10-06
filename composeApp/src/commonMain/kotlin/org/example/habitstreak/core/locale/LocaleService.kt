package org.example.habitstreak.core.locale

class LocaleService(
    private val localeRepository: ILocaleRepository,
    private val localeStateHolder: ILocaleStateHolder
) : ILocaleService {

    override suspend fun changeLocale(locale: AppLocale) {
        localeStateHolder.setCurrentLocale(locale)
        localeRepository.setLocale(locale.code)
        // Update the app-wide locale environment for stringResource()
        changeAppLocale(locale.code)
    }

    override suspend fun initializeLocale() {
        val savedLocaleCode = localeRepository.getLocale()
        val savedLocale = AppLocale.fromCode(savedLocaleCode)

        // Always use the saved locale (which includes system locale from first launch)
        localeStateHolder.setCurrentLocale(savedLocale)
        changeAppLocale(savedLocale.code)
    }

    override fun getAvailableLocales(): List<AppLocale> {
        return AppLocale.entries
    }

    override fun getSystemLocale(): AppLocale {
        val systemLocaleCode = SystemLocaleProvider.getSystemLocaleCode()
        return LocaleMatchingService.findBestMatch(systemLocaleCode)
    }
}