package org.example.habitstreak.core.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeStateHolder : IThemeStateHolder {
    private val _currentTheme = MutableStateFlow(AppTheme.SYSTEM)
    override val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    override fun getCurrentTheme(): AppTheme = _currentTheme.value

    override fun setCurrentTheme(theme: AppTheme) {
        _currentTheme.value = theme
    }
}