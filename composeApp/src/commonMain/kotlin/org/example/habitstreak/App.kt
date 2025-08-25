package org.example.habitstreak

import androidx.compose.runtime.Composable
import org.example.habitstreak.presentation.navigation.AppNavigation
import org.example.habitstreak.presentation.ui.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        AppNavigation()
    }
}