package org.example.habitstreak.core.theme

import kotlinx.coroutines.flow.StateFlow

interface IThemeStateHolder {
    val currentTheme: StateFlow<AppTheme>
    fun getCurrentTheme(): AppTheme
    fun setCurrentTheme(theme: AppTheme)
}