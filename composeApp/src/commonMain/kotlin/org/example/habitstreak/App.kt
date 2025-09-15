package org.example.habitstreak

import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import org.example.habitstreak.core.util.AppLocale
import org.example.habitstreak.core.util.LocaleManager
import org.example.habitstreak.core.util.LocaleProvider
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

    // Debug log for locale changes
    LaunchedEffect(currentLocale) {
        println("🎯 App: currentLocale changed to ${currentLocale.code}")
    }

    // Initialize predefined categories and load saved locale when app starts
    LaunchedEffect(Unit) {
        initializeCategoriesUseCase()

        // Load saved locale
        val savedLocaleCode = preferencesRepository.getLocale().first()
        val savedLocale = AppLocale.fromCode(savedLocaleCode)
        println("🚀 App: Loading saved locale: ${savedLocale.code}")
        LocaleManager.setLocale(savedLocale)
    }

    println("🌍 App: Providing locale ${currentLocale.code} to LocaleProvider")
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