package org.example.habitstreak.presentation.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS doesn't have a system back button like Android
    // Back navigation is handled through NavigationView or swipe gestures
    // This is intentionally a no-op for iOS
}