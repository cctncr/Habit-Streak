package org.example.habitstreak.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.example.habitstreak.core.theme.AppTheme as CoreAppTheme
import org.example.habitstreak.core.theme.IThemeStateHolder
import org.example.habitstreak.core.theme.getColorSchemeForTheme
import org.koin.compose.koinInject

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    val themeStateHolder: IThemeStateHolder = koinInject()
    val currentTheme by themeStateHolder.currentTheme.collectAsState()
    val isSystemInDarkMode = isSystemInDarkTheme()

    val colorScheme = getColorSchemeForTheme(
        appTheme = currentTheme,
        isSystemInDarkMode = isSystemInDarkMode
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}