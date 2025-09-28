package org.example.habitstreak.presentation.ui.state

import org.example.habitstreak.core.locale.AppLocale
import org.example.habitstreak.core.theme.AppTheme
import org.example.habitstreak.presentation.permission.PermissionContext

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val locale: AppLocale = AppLocale.ENGLISH,
    val isLoading: Boolean = false,
    val message: String? = null,
    // Permission-related states
    val showPermissionDialog: Boolean = false,
    val permissionDialogContext: PermissionContext? = null,
    val showPermissionSettingsDialog: Boolean = false,
    val showPermissionSoftDenialDialog: Boolean = false,
    val permissionMessage: String? = null,
    val hasNotificationPermission: Boolean = false
)
