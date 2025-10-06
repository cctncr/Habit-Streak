package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.usecase.notification.GlobalNotificationUseCase
import org.example.habitstreak.domain.usecase.notification.NotificationOperationResult
import org.example.habitstreak.domain.usecase.notification.NotificationPreferencesUseCase
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
    private val localeService: ILocaleService,
    private val localeStateHolder: ILocaleStateHolder,
    private val themeService: IThemeService,
    private val themeStateHolder: IThemeStateHolder,
    private val permissionFlowHandler: PermissionFlowHandler,
    private val globalNotificationUseCase: GlobalNotificationUseCase,
    private val notificationPreferencesUseCase: NotificationPreferencesUseCase
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

    fun toggleNotifications(
        enabled: Boolean,
        onPermissionFlowResult: (PermissionFlowResult) -> Unit
    ) {
        println("🔔 SETTINGS_VIEWMODEL: toggleNotifications called with enabled=$enabled")
        if (enabled) {
            println("🔔 SETTINGS_VIEWMODEL: Requesting notification permission")
            requestNotificationPermission(onPermissionFlowResult)
        } else {
            println("🔔 SETTINGS_VIEWMODEL: Disabling notifications")
            disableNotifications()
        }
    }

    private fun requestNotificationPermission(
        onResult: (PermissionFlowResult) -> Unit
    ) {
        println("🔔 SETTINGS_VIEWMODEL: requestNotificationPermission started")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            println("🔔 SETTINGS_VIEWMODEL: UI loading state set to true")

            permissionFlowHandler.requestPermissionWithFlow { result ->
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = false) }

                    when (result) {
                        is PermissionFlowResult.Granted,
                        is PermissionFlowResult.PermissionGranted -> {
                            println("🔔 SETTINGS_VIEWMODEL: Permission granted, enabling notifications")
                            enableNotifications()
                        }
                        else -> {
                            println("🔔 SETTINGS_VIEWMODEL: Permission flow result: ${result::class.simpleName}")
                            onResult(result)
                        }
                    }
                }
            }
        }
    }

    private fun disableNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Use GlobalNotificationUseCase.disable instead of duplicate logic
                when (val result = globalNotificationUseCase.disable()) {
                    is NotificationOperationResult.Success -> {
                        println("🔔 SETTINGS_VIEWMODEL: Notifications disabled successfully")
                    }
                    is NotificationOperationResult.PartialSuccess -> {
                        println("🔔 SETTINGS_VIEWMODEL: Partial success disabling - ${result.successCount} succeeded, ${result.failureCount} failed")
                    }
                    is NotificationOperationResult.Error -> {
                        println("🔔 SETTINGS_VIEWMODEL: Error disabling notifications: ${result.error.message}")
                    }
                    is NotificationOperationResult.PermissionRequired -> {
                        println("🔔 SETTINGS_VIEWMODEL: Unexpected permission required for disable")
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

    private suspend fun enableNotifications() {
        try {
            // Use GlobalNotificationUseCase.enable instead of duplicate logic
            when (val result = globalNotificationUseCase.enable()) {
                is NotificationOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notificationsEnabled = true,
                            hasNotificationPermission = true,
                            message = "Notifications enabled"
                        )
                    }
                }
                is NotificationOperationResult.PartialSuccess -> {
                    val total = result.successCount + result.failureCount
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notificationsEnabled = true,
                            hasNotificationPermission = true,
                            message = "Enabled ${result.successCount} of $total habit notifications. ${result.failureCount} failed."
                        )
                    }
                }
                is NotificationOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Failed to enable notifications: ${result.error.message}"
                        )
                    }
                }
                is NotificationOperationResult.PermissionRequired -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Permission required"
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

    // Duplicate methods removed - now using GlobalNotificationUseCase

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            try {
                // Use NotificationPreferencesUseCase to update preference and sync notifications
                notificationPreferencesUseCase.updateSound(enabled)
                println("🔔 SETTINGS_VIEWMODEL: Sound preference updated to $enabled and notifications synced")
            } catch (e: Exception) {
                println("🔔 SETTINGS_VIEWMODEL: Error updating sound preference: ${e.message}")
                _uiState.update { it.copy(message = "Failed to update sound preference") }
            }
        }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch {
            try {
                // Use NotificationPreferencesUseCase to update preference and sync notifications
                notificationPreferencesUseCase.updateVibration(enabled)
                println("🔔 SETTINGS_VIEWMODEL: Vibration preference updated to $enabled and notifications synced")
            } catch (e: Exception) {
                println("🔔 SETTINGS_VIEWMODEL: Error updating vibration preference: ${e.message}")
                _uiState.update { it.copy(message = "Failed to update vibration preference") }
            }
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

    // Call this when permission is granted outside the app (e.g., from system settings)
    fun onPermissionGrantedExternally() {
        viewModelScope.launch {
            val hasPermission = permissionFlowHandler.hasPermission()
            _uiState.update {
                it.copy(hasNotificationPermission = hasPermission)
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