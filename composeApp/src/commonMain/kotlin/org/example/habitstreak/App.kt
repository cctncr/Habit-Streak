package org.example.habitstreak

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.example.habitstreak.di.InitializeCategoriesUseCase
import org.example.habitstreak.presentation.navigation.AppNavigation
import org.example.habitstreak.presentation.ui.theme.AppTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    val initializeCategoriesUseCase: InitializeCategoriesUseCase = koinInject()

    // Initialize predefined categories when app starts
    LaunchedEffect(Unit) {
        initializeCategoriesUseCase()
    }

    AppTheme {
        AppNavigation()
    }
}