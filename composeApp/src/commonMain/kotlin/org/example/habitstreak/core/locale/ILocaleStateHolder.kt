package org.example.habitstreak.core.locale

import kotlinx.coroutines.flow.StateFlow
import org.example.habitstreak.core.util.AppLocale

interface ILocaleStateHolder {
    val currentLocale: StateFlow<AppLocale>
    fun getCurrentLocale(): AppLocale
    fun setCurrentLocale(locale: AppLocale)
}