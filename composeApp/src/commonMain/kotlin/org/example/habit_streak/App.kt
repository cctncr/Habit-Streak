package org.example.habit_streak

import androidx.compose.runtime.Composable
import org.example.habit_streak.presentation.navigation.AppNavigation
import org.example.habit_streak.presentation.ui.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        AppNavigation()
    }
}