package org.example.habitstreak.core.locale

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.habitstreak.core.locale.AppLocale

class LocaleStateHolder : ILocaleStateHolder {
    private val _currentLocale = MutableStateFlow(AppLocale.ENGLISH)
    override val currentLocale: StateFlow<AppLocale> = _currentLocale.asStateFlow()

    override fun getCurrentLocale(): AppLocale = _currentLocale.value

    override fun setCurrentLocale(locale: AppLocale) {
        _currentLocale.value = locale
    }
}