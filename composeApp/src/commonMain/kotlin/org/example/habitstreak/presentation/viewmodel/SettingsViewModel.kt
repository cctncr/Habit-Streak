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

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val notificationService: NotificationService?,
    private val habitRepository: HabitRepository
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
                preferencesRepository.getTheme().collect { theme ->
                    _uiState.update { it.copy(theme = theme) }
                }
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Save preference
                preferencesRepository.setNotificationsEnabled(enabled)

                if (notificationService != null) {
                    if (enabled) {
                        // Re-enable all habit notifications
                        enableAllHabitNotifications()
                    } else {
                        // Disable all habit notifications
                        disableAllHabitNotifications()
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = if (enabled) "Notifications enabled" else "Notifications disabled"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Failed to update notifications: ${e.message}"
                    )
                }
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
                        message = "Time to ${habit.title}"
                    )
                } catch (e: Exception) {
                    // Log error but continue with other habits
                    println("Failed to enable notification for habit ${habit.id}: ${e.message}")
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
                println("Failed to disable notification for habit ${habit.id}: ${e.message}")
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

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesRepository.setTheme(theme)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}