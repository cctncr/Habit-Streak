package org.example.habitstreak.presentation.ui.state

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val theme: String = "system",
    val isLoading: Boolean = false,
    val message: String? = null
)
