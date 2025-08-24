package org.example.habit_streak.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF27AE60),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9),
    secondary = Color(0xFF2196F3),
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onBackground = Color(0xFF2C3E50),
    onSurface = Color(0xFF2C3E50),
    surfaceVariant = Color(0xFFF5F5F5),
    error = Color(0xFFE74C3C)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF27AE60),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E5E3A),
    secondary = Color(0xFF2196F3),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    error = Color(0xFFCF6679)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}