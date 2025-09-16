package org.example.habitstreak.core.theme

interface IThemeService {
    suspend fun changeTheme(theme: AppTheme)
    suspend fun initializeTheme()
    fun getAvailableThemes(): List<AppTheme>
    fun getSystemTheme(): AppTheme
    fun isSystemInDarkMode(): Boolean
}