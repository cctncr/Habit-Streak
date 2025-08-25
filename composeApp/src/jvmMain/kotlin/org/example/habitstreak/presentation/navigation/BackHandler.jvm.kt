package org.example.habitstreak.presentation.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop doesn't have system back button
    // Can be implemented with keyboard shortcuts if needed
}