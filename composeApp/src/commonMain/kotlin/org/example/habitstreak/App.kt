package org.example.habitstreak

import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import org.example.habitstreak.core.util.AppLocale
import org.example.habitstreak.core.util.LocaleProvider
import org.example.habitstreak.core.locale.ILocaleService
import org.example.habitstreak.core.locale.ILocaleStateHolder
import org.example.habitstreak.core.locale.AppEnvironment
import org.example.habitstreak.domain.usecase.InitializeCategoriesUseCase
import org.example.habitstreak.presentation.navigation.AppNavigation
import org.example.habitstreak.presentation.ui.theme.AppTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    val initializeCategoriesUseCase: InitializeCategoriesUseCase = koinInject()
    val localeService: ILocaleService = koinInject()
    val localeStateHolder: ILocaleStateHolder = koinInject()
    val currentLocale by localeStateHolder.currentLocale.collectAsState()

    // Initialize predefined categories and locale when app starts
    LaunchedEffect(Unit) {
        initializeCategoriesUseCase()
        localeService.initializeLocale()
    }

    // Use AppEnvironment to enable stringResource() locale changes
    AppEnvironment {
        LocaleProvider(locale = currentLocale) {
            AppTheme {
                AppNavigation()
            }
        }
    }
}