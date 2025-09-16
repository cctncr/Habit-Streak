package org.example.habitstreak.presentation.ui.state

import org.example.habitstreak.core.util.AppLocale
import org.example.habitstreak.core.theme.AppTheme

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val locale: AppLocale = AppLocale.ENGLISH,
    val isLoading: Boolean = false,
    val message: String? = null
)
