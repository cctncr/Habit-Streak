package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.presentation.ui.state.SettingsUiState
import org.example.habitstreak.core.locale.AppLocale
import org.example.habitstreak.core.locale.ILocaleService
import org.example.habitstreak.core.locale.ILocaleStateHolder
import org.example.habitstreak.core.theme.AppTheme
import org.example.habitstreak.core.theme.IThemeService
import org.example.habitstreak.core.theme.IThemeStateHolder
import org.example.habitstreak.presentation.permission.PermissionContext
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
    private val permissionFlowHandler: PermissionFlowHandler
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
        if (enabled) {
            requestNotificationPermission()
        } else {
            disableNotifications()
        }
    }

    private fun requestNotificationPermission() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            permissionFlowHandler.requestPermissionWithFlow(
                context = PermissionContext.SETTINGS,
                onResult = { result ->
                    handlePermissionFlowResult(result)
                }
            )
        }
    }

    private fun disableNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Save preference
                preferencesRepository.setNotificationsEnabled(false)

                if (notificationService != null) {
                    // Disable all habit notifications
                    disableAllHabitNotifications()
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
        viewModelScope.launch {
            when (result) {
                is PermissionFlowResult.Granted -> {
                    enableNotifications()
                }

                is PermissionFlowResult.PermissionGranted -> {
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showPermissionDialog = true,
                            permissionDialogContext = result.context,
                            permissionMessage = result.rationaleMessage
                        )
                    }
                }

                is PermissionFlowResult.ShowSoftDenialDialog -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showPermissionSoftDenialDialog = true,
                            permissionMessage = result.message
                        )
                    }
                }

                is PermissionFlowResult.ShowSettingsDialog -> {
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
            // Save preference
            preferencesRepository.setNotificationsEnabled(true)

            if (notificationService != null) {
                // Re-enable all habit notifications
                enableAllHabitNotifications()
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    notificationsEnabled = true,
                    hasNotificationPermission = true,
                    message = "Notifications enabled"
                )
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

    private suspend fun enableAllHabitNotifications() {
        if (notificationService == null) return

        // Get all habits with reminders
        habitRepository.observeActiveHabits().first().forEach { habit ->
            if (habit.isReminderEnabled && !habit.reminderTime.isNullOrEmpty()) {
                try {
                    val time = LocalTime.parse(habit.reminderTime)
                    notificationService.enableNotification(
                        habitId = habit.id,
                        time = time,
                        message = "Time to ${habit.title}" // TODO: Use string resources
                    )
                } catch (e: Exception) {
                    // Log error but continue with other habits
                    // TODO: Implement proper logging framework
                }
            }
        }
    }

    private suspend fun disableAllHabitNotifications() {
        if (notificationService == null) return

        // Cancel all notifications
        habitRepository.observeActiveHabits().first().forEach { habit ->
            try {
                notificationService.disableNotification(habit.id)
            } catch (e: Exception) {
                // TODO: Implement proper logging framework
            }
        }
    }

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
                permissionDialogContext = null,
                permissionMessage = null
            )
        }
    }

    fun onPermissionRequested() {
        viewModelScope.launch {
            val currentContext = _uiState.value.permissionDialogContext ?: PermissionContext.SETTINGS

            onPermissionDialogDismiss()

            permissionFlowHandler.handleSystemPermissionResult(
                context = currentContext,
                onResult = { result ->
                    handlePermissionFlowResult(result)
                }
            )
        }
    }

    fun onNeverAskAgain() {
        val currentContext = _uiState.value.permissionDialogContext ?: PermissionContext.SETTINGS
        permissionFlowHandler.handleNeverAskAgain(currentContext)
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
                val success = permissionFlowHandler.handleOpenSettings(PermissionContext.SETTINGS)
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