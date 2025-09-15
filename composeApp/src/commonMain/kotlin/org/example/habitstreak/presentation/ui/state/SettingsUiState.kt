package org.example.habitstreak.presentation.ui.state

import org.example.habitstreak.core.util.AppLocale

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val theme: String = "system",
    val locale: AppLocale = AppLocale.ENGLISH,
    val isLoading: Boolean = false,
    val message: String? = null
)
