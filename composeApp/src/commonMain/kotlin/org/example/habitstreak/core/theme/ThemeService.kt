package org.example.habitstreak.core.theme

import org.example.habitstreak.core.theme.changeAppTheme

class ThemeService(
    private val themeRepository: IThemeRepository,
    private val themeStateHolder: IThemeStateHolder
) : IThemeService {

    override suspend fun changeTheme(theme: AppTheme) {
        println("🎨 ThemeService.changeTheme: Changing to ${theme.code}")
        themeStateHolder.setCurrentTheme(theme)
        themeRepository.setTheme(theme.code)
        // Update the app-wide theme environment
        changeAppTheme(theme)
        println("✅ ThemeService.changeTheme: Completed change to ${theme.code}")
    }

    override suspend fun initializeTheme() {
        val savedThemeCode = themeRepository.getTheme()
        val savedTheme = AppTheme.fromCode(savedThemeCode)

        // Always use the saved theme (which includes system theme from first launch)
        themeStateHolder.setCurrentTheme(savedTheme)
        changeAppTheme(savedTheme)

        // Debug logging to check what's happening
        println("🎨 ThemeService.initializeTheme: savedThemeCode='$savedThemeCode', using theme='${savedTheme.code}'")
    }

    override fun getAvailableThemes(): List<AppTheme> {
        return AppTheme.getAvailableThemes()
    }

    override fun getSystemTheme(): AppTheme {
        return if (isSystemInDarkMode()) AppTheme.DARK else AppTheme.LIGHT
    }

    override fun isSystemInDarkMode(): Boolean {
        return SystemThemeProvider.isSystemInDarkMode()
    }
}