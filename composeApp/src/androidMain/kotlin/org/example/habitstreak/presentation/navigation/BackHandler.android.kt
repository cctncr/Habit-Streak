package org.example.habitstreak.presentation.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS handles back navigation through NavigationView
    // No-op for now, can be implemented with custom gesture handling if needed
}