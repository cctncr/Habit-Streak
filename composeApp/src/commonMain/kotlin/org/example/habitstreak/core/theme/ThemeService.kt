package org.example.habitstreak.core.theme

class ThemeService(
    private val themeRepository: IThemeRepository,
    private val themeStateHolder: IThemeStateHolder
) : IThemeService {

    override suspend fun changeTheme(theme: AppTheme) {
        themeStateHolder.setCurrentTheme(theme)
        themeRepository.setTheme(theme.code)
        // Update the app-wide theme environment
        changeAppTheme(theme)
    }

    override suspend fun initializeTheme() {
        val savedThemeCode = themeRepository.getTheme()
        val savedTheme = AppTheme.fromCode(savedThemeCode)

        // Always use the saved theme (which includes system theme from first launch)
        themeStateHolder.setCurrentTheme(savedTheme)
        changeAppTheme(savedTheme)
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