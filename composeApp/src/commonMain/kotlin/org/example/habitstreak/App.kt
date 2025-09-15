package org.example.habitstreak

import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import org.example.habitstreak.core.util.AppLocale
import org.example.habitstreak.core.util.LocaleManager
import org.example.habitstreak.core.util.LocaleProvider
import org.example.habitstreak.core.util.SystemLocaleProvider
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.usecase.InitializeCategoriesUseCase
import org.example.habitstreak.presentation.navigation.AppNavigation
import org.example.habitstreak.presentation.ui.theme.AppTheme
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.first

@Composable
fun App() {
    val initializeCategoriesUseCase: InitializeCategoriesUseCase = koinInject()
    val preferencesRepository: PreferencesRepository = koinInject()
    val currentLocale by LocaleManager.currentLocale.collectAsState()

    println("🔄 App: currentLocale state = ${currentLocale.code}")

    // Initialize predefined categories and load saved locale when app starts
    LaunchedEffect(Unit) {
        initializeCategoriesUseCase()

        // Load saved locale or system locale on first launch
        val savedLocaleCode = preferencesRepository.getLocale().first()
        val savedLocale = AppLocale.fromCode(savedLocaleCode)

        println("🚀 App: Loading locale: ${savedLocale.code}")

        // If no saved locale preference, detect and save system locale
        if (savedLocaleCode == SystemLocaleProvider.getSystemLocaleCode() &&
            savedLocaleCode != "en") { // Only log if it's not the default English fallback
            println("🌍 App: First launch detected, using system locale: ${savedLocale.code}")
        }

        LocaleManager.setLocale(savedLocale)
    }

    println("🌍 App: Providing locale ${currentLocale.code} to LocaleProvider")
    // Use key to force recomposition when locale changes
    key(currentLocale.code) {
        LocaleProvider(locale = currentLocale) {
            AppTheme {
                AppNavigation(
                    onLocaleChanged = { locale ->
                        // LocaleManager.setLocale is already called by SettingsViewModel
                        // currentLocale will be automatically updated through the StateFlow
                    }
                )
            }
        }
    }
}