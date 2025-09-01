package org.example.habitstreak.presentation.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS handles back navigation through NavigationController
    // This is a no-op on iOS as the system handles it
}