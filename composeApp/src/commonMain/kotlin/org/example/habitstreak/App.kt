package org.example.habitstreak

import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import org.example.habitstreak.core.locale.AppLocale
import org.example.habitstreak.core.locale.LocaleProvider
import org.example.habitstreak.core.locale.ILocaleService
import org.example.habitstreak.core.locale.ILocaleStateHolder
import org.example.habitstreak.core.locale.AppEnvironment
import org.example.habitstreak.core.theme.IThemeService
import org.example.habitstreak.core.theme.AppThemeEnvironment
import org.example.habitstreak.domain.usecase.InitializeCategoriesUseCase
import org.example.habitstreak.presentation.navigation.AppNavigation
import org.example.habitstreak.presentation.ui.theme.AppTheme
import org.koin.compose.koinInject

@Composable
fun App(
    deepLinkHabitId: String? = null,
    shouldNavigateToHabit: Boolean = false,
    onDeepLinkHandled: () -> Unit = {},
    onFirstFrameRendered: () -> Unit = {}
) {
    val initializeCategoriesUseCase: InitializeCategoriesUseCase = koinInject()
    val localeService: ILocaleService = koinInject()
    val themeService: IThemeService = koinInject()
    val localeStateHolder: ILocaleStateHolder = koinInject()
    val currentLocale by localeStateHolder.currentLocale.collectAsState()

    var isInitialized by remember { mutableStateOf(false) }

    // Initialize predefined categories, locale and theme when app starts
    LaunchedEffect(Unit) {
        initializeCategoriesUseCase()
        localeService.initializeLocale()
        themeService.initializeTheme()
        // Mark as initialized after all initialization tasks complete
        isInitialized = true
    }

    // Don't render anything until initialization is complete
    // This keeps the native splash screen visible (iOS LaunchScreen / Android SplashScreen)
    if (!isInitialized) {
        return
    }

    // Use both AppEnvironment and AppThemeEnvironment to enable runtime changes
    AppEnvironment {
        AppThemeEnvironment {
            LocaleProvider(locale = currentLocale) {
                AppTheme {
                    AppNavigation(
                        deepLinkHabitId = deepLinkHabitId,
                        shouldNavigateToHabit = shouldNavigateToHabit,
                        onDeepLinkHandled = onDeepLinkHandled,
                        onFirstFrameRendered = onFirstFrameRendered
                    )
                }
            }
        }
    }
}