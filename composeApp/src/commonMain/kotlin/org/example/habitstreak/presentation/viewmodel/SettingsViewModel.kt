package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.usecase.notification.EnableGlobalNotificationsUseCase
import org.example.habitstreak.domain.usecase.notification.DisableGlobalNotificationsUseCase
import org.example.habitstreak.presentation.ui.state.SettingsUiState
import org.example.habitstreak.core.locale.AppLocale
import org.example.habitstreak.core.locale.ILocaleService
import org.example.habitstreak.core.locale.ILocaleStateHolder
import org.example.habitstreak.core.theme.AppTheme
import org.example.habitstreak.core.theme.IThemeService
import org.example.habitstreak.core.theme.IThemeStateHolder
import org.example.habitstreak.presentation.permission.PermissionFlowHandler
import org.example.habitstreak.presentation.permission.PermissionFlowResult

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val notificationService: NotificationService?,
    private val habitRepository: HabitRepository,
    private val localeService: ILocaleService,
    private val localeStateHolder: ILocaleStateHolder,
    private val themeService: IThemeService,
    private val themeStateHolder: IThemeStateHolder,
    private val permissionFlowHandler: PermissionFlowHandler,
    private val enableGlobalNotificationsUseCase: EnableGlobalNotificationsUseCase,
    private val disableGlobalNotificationsUseCase: DisableGlobalNotificationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            launch {
                preferencesRepository.isNotificationsEnabled().collect { enabled ->
                    _uiState.update { it.copy(notificationsEnabled = enabled) }
                }
            }

            launch {
                preferencesRepository.isSoundEnabled().collect { enabled ->
                    _uiState.update { it.copy(soundEnabled = enabled) }
                }
            }

            launch {
                preferencesRepository.isVibrationEnabled().collect { enabled ->
                    _uiState.update { it.copy(vibrationEnabled = enabled) }
                }
            }

            launch {
                themeStateHolder.currentTheme.collect { theme ->
                    _uiState.update { it.copy(theme = theme) }
                }
            }

            launch {
                localeStateHolder.currentLocale.collect { locale ->
                    _uiState.update { it.copy(locale = locale) }
                }
            }

            launch {
                // Check permission status
                val hasPermission = permissionFlowHandler.hasPermission()
                _uiState.update { it.copy(hasNotificationPermission = hasPermission) }
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        println("🔔 SETTINGS_VIEWMODEL: toggleNotifications called with enabled=$enabled")
        if (enabled) {
            println("🔔 SETTINGS_VIEWMODEL: Requesting notification permission")
            requestNotificationPermission()
        } else {
            println("🔔 SETTINGS_VIEWMODEL: Disabling notifications")
            disableNotifications()
        }
    }

    private fun requestNotificationPermission() {
        println("🔔 SETTINGS_VIEWMODEL: requestNotificationPermission started")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            println("🔔 SETTINGS_VIEWMODEL: UI loading state set to true")

            permissionFlowHandler.requestPermissionWithFlow(
                onResult = { result ->
                    println("🔔 SETTINGS_VIEWMODEL: Permission flow result received: ${result::class.simpleName}")
                    handlePermissionFlowResult(result)
                }
            )
        }
    }

    private fun disableNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Use DisableGlobalNotificationsUseCase instead of duplicate logic
                when (val result = disableGlobalNotificationsUseCase.execute()) {
                    is DisableGlobalNotificationsUseCase.GlobalDisableResult.Success -> {
                        println("🔔 SETTINGS_VIEWMODEL: Notifications disabled successfully")
                    }
                    is DisableGlobalNotificationsUseCase.GlobalDisableResult.Error -> {
                        println("🔔 SETTINGS_VIEWMODEL: Error disabling notifications: ${result.message}")
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Notifications disabled"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Failed to disable notifications: ${e.message}"
                    )
                }
            }
        }
    }

    private fun handlePermissionFlowResult(result: PermissionFlowResult) {
        println("🔔 SETTINGS_VIEWMODEL: handlePermissionFlowResult called with: ${result::class.simpleName}")
        viewModelScope.launch {
            when (result) {
                is PermissionFlowResult.Granted -> {
                    println("🔔 SETTINGS_VIEWMODEL: Permission granted, enabling notifications")
                    enableNotifications()
                }

                is PermissionFlowResult.PermissionGranted -> {
                    println("🔔 SETTINGS_VIEWMODEL: PermissionGranted with message: ${result.message}")
                    enableNotifications()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = result.message,
                            hasNotificationPermission = true
                        )
                    }
                }

                is PermissionFlowResult.ShowRationaleDialog -> {
                    println("🔔 SETTINGS_VIEWMODEL: ShowRationaleDialog, showing dialog")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showPermissionDialog = true,
                            permissionMessage = result.rationaleMessage
                        )
                    }
                }

                is PermissionFlowResult.ShowSoftDenialDialog -> {
                    println("🔔 SETTINGS_VIEWMODEL: ShowSoftDenialDialog, showing denial dialog")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showPermissionSoftDenialDialog = true,
                            permissionMessage = result.message
                        )
                    }
                }

                is PermissionFlowResult.ShowSettingsDialog -> {
                    println("🔔 SETTINGS_VIEWMODEL: ShowSettingsDialog, showing settings dialog")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showPermissionSettingsDialog = true,
                            permissionMessage = result.message
                        )
                    }
                }

                is PermissionFlowResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = result.message
                        )
                    }
                }
            }
        }
    }

    private suspend fun enableNotifications() {
        try {
            // Use EnableGlobalNotificationsUseCase instead of duplicate logic
            when (val result = enableGlobalNotificationsUseCase.execute()) {
                is EnableGlobalNotificationsUseCase.GlobalEnableResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notificationsEnabled = true,
                            hasNotificationPermission = true,
                            message = "Notifications enabled"
                        )
                    }
                }
                is EnableGlobalNotificationsUseCase.GlobalEnableResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Failed to enable notifications: ${result.message}"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    message = "Failed to enable notifications: ${e.message}"
                )
            }
        }
    }

    // Duplicate methods removed - now using EnableGlobalNotificationsUseCase and DisableGlobalNotificationsUseCase

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSoundEnabled(enabled)
        }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setVibrationEnabled(enabled)
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            themeService.changeTheme(theme)
        }
    }

    fun setLocale(locale: AppLocale) {
        viewModelScope.launch {
            localeService.changeLocale(locale)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    // Permission dialog handlers
    fun onPermissionDialogDismiss() {
        _uiState.update {
            it.copy(
                showPermissionDialog = false,
                permissionMessage = null
            )
        }
    }

    fun onPermissionRequested() {
        viewModelScope.launch {
            onPermissionDialogDismiss()

            permissionFlowHandler.handleSystemPermissionResult(
                onResult = { result ->
                    handlePermissionFlowResult(result)
                }
            )
        }
    }

    fun onNeverAskAgain() {
        permissionFlowHandler.handleNeverAskAgain()
        onPermissionDialogDismiss()
    }

    fun onPermissionSettingsDialogDismiss() {
        _uiState.update {
            it.copy(
                showPermissionSettingsDialog = false,
                permissionMessage = null
            )
        }
    }

    fun onOpenDeviceSettings() {
        viewModelScope.launch {
            try {
                val success = permissionFlowHandler.handleOpenSettings()
                if (!success) {
                    _uiState.update {
                        it.copy(message = "Unable to open device settings. Please open Settings manually and navigate to notifications.")
                    }
                }
                // Always dismiss the dialog regardless of success
                onPermissionSettingsDialogDismiss()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(message = "Error opening settings: ${e.message}")
                }
                onPermissionSettingsDialogDismiss()
            }
        }
    }

    fun onDisableNotificationFeature() {
        disableNotifications()
        onPermissionSettingsDialogDismiss()
    }

    fun onPermissionSoftDenialDismiss() {
        _uiState.update {
            it.copy(
                showPermissionSoftDenialDialog = false,
                permissionMessage = null
            )
        }
    }

    fun onPermissionRetry() {
        onPermissionSoftDenialDismiss()
        requestNotificationPermission()
    }

    // Call this when permission is granted outside the app (e.g., from system settings)
    fun onPermissionGrantedExternally() {
        viewModelScope.launch {
            val hasPermission = permissionFlowHandler.hasPermission()
            _uiState.update {
                it.copy(
                    hasNotificationPermission = hasPermission,
                    showPermissionSettingsDialog = false,
                    showPermissionSoftDenialDialog = false
                )
            }

            if (hasPermission && _uiState.value.notificationsEnabled) {
                enableNotifications()
            }
        }
    }

    // Call this when app comes to foreground to refresh permission state
    fun refreshPermissionState() {
        permissionFlowHandler.invalidateCache()
        viewModelScope.launch {
            val hasPermission = permissionFlowHandler.hasPermission()
            _uiState.update { it.copy(hasNotificationPermission = hasPermission) }
        }
    }
}