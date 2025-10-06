package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.NotificationError
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.usecase.habit.CalculateHabitStatsUseCase
import org.example.habitstreak.domain.model.NotificationPeriod
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.usecase.notification.HabitNotificationUseCase
import org.example.habitstreak.domain.usecase.notification.GlobalNotificationUseCase
import org.example.habitstreak.domain.usecase.notification.NotificationPreferencesUseCase
import org.example.habitstreak.domain.usecase.notification.NotificationOperationResult
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.presentation.model.YearMonth
import org.example.habitstreak.presentation.ui.state.HabitDetailUiState
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class HabitDetailViewModel(
    private val habitId: String,
    private val habitRepository: HabitRepository,
    private val habitRecordRepository: HabitRecordRepository,
    private val calculateHabitStatsUseCase: CalculateHabitStatsUseCase,
    private val habitNotificationUseCase: HabitNotificationUseCase,
    private val globalNotificationUseCase: GlobalNotificationUseCase,
    private val notificationPreferencesUseCase: NotificationPreferencesUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val dateProvider: DateProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    init {
        loadData()
        observeNotificationConfig()
        loadNotificationPreferences()
        observeGlobalNotificationStatus()
    }

    fun loadData() {
        val today = dateProvider.today()

        _uiState.update {
            it.copy(
                selectedMonth = YearMonth(
                    today.year,
                    today.month.number
                ),
                isLoading = true
            )
        }

        loadHabitData()
        loadRecords()
    }

    private fun loadHabitData() {
        viewModelScope.launch {
            habitRepository.observeHabitById(habitId).collect { habit: Habit? ->
                _uiState.update { it.copy(habit = habit) }

                if (habit != null) {
                    calculateStatistics()
                }
            }
        }
    }

    private fun loadRecords() {
        viewModelScope.launch {
            habitRecordRepository.observeRecordsForHabit(habitId).collect { records ->
                _uiState.update { it.copy(records = records, isLoading = false) }
                calculateStatistics()
            }
        }
    }

    private fun calculateStatistics() {
        viewModelScope.launch {
            val habit = _uiState.value.habit ?: return@launch
            val records = _uiState.value.records

            calculateHabitStatsUseCase(habitId, habit, records).fold(
                onSuccess = { stats ->
                    _uiState.update { state ->
                        state.copy(
                            stats = HabitStats(
                                currentStreak = stats.currentStreak,
                                longestStreak = stats.longestStreak,
                                totalDays = stats.totalDays,
                                completionRate = stats.completionRate,
                                averagePerDay = stats.averagePerDay,
                                thisWeekCount = stats.thisWeekCount,
                                thisMonthCount = stats.thisMonthCount,
                                lastCompleted = stats.lastCompleted
                            )
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun changeMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(selectedMonth = yearMonth) }
    }

    fun updateProgress(date: LocalDate, completedCount: Int, note: String? = null) {
        viewModelScope.launch {
            if (completedCount == 0 && note.isNullOrBlank()) {
                habitRecordRepository.markHabitAsIncomplete(habitId, date).fold(
                    onSuccess = { calculateStatistics() },
                    onFailure = { error -> _uiState.update { it.copy(error = error.message) } }
                )
            } else {
                habitRecordRepository.markHabitAsComplete(
                    habitId = habitId,
                    date = date,
                    count = completedCount,
                    note = note ?: ""
                ).fold(
                    onSuccess = { calculateStatistics() },
                    onFailure = { error -> _uiState.update { it.copy(error = error.message) } }
                )
            }
        }
    }

    fun deleteRecord(date: LocalDate) {
        viewModelScope.launch {
            habitRecordRepository.markHabitAsIncomplete(habitId, date).fold(
                onSuccess = {
                    loadRecords()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to delete record")
                    }
                }
            )
        }
    }

    fun deleteHabit() {
        viewModelScope.launch {
            habitRepository.deleteHabit(habitId).fold(
                onSuccess = {
                    // Navigation will be handled by the screen
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to delete habit")
                    }
                }
            )
        }
    }

    private fun observeNotificationConfig() {
        viewModelScope.launch {
            habitNotificationUseCase.observe(habitId).collect { config ->
                val currentError = _uiState.value.notificationError
                _uiState.update { state ->
                    state.copy(
                        notificationConfig = config,
                        isNotificationEnabled = config?.isEnabled == true,
                        notificationTime = config?.time,
                        notificationPeriod = config?.period ?: NotificationPeriod.EveryDay,
                        notificationError = if (currentError?.shouldPreserve() == true) currentError else null
                    )
                }
            }
        }
    }

    fun toggleNotification(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {

                // First check global notification status
                when (globalNotificationUseCase.checkStatus()) {
                    is GlobalNotificationUseCase.GlobalStatus.NeedsSystemPermission -> {
                        _uiEvents.tryEmit(UiEvent.RequestNotificationPermission)
                        _uiState.update { it.copy(isNotificationEnabled = false) }
                        return@launch
                    }

                    is GlobalNotificationUseCase.GlobalStatus.NeedsGlobalEnable -> {
                        _uiEvents.tryEmit(UiEvent.ShowGlobalEnableDialog(_uiState.value.habit?.title))
                        _uiState.update { it.copy(isNotificationEnabled = false) }
                        return@launch
                    }

                    is GlobalNotificationUseCase.GlobalStatus.AlreadyEnabled,
                    is GlobalNotificationUseCase.GlobalStatus.CanEnable -> {
                        // Proceed with normal notification enabling
                    }
                }

                // Enable habit notification
                val time = _uiState.value.notificationTime ?: LocalTime(9, 0)
                when (val result = habitNotificationUseCase.enable(habitId, time)) {
                    is NotificationOperationResult.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                isNotificationEnabled = true,
                                notificationTime = time,
                                notificationError = null
                            )
                        }
                    }
                    is NotificationOperationResult.Error -> {
                        setNotificationError(result.error)
                        _uiState.update { it.copy(isNotificationEnabled = false) }

                        if (result.error is NotificationError.PermissionDenied && result.error.canRequestAgain) {
                            _uiEvents.tryEmit(UiEvent.RequestNotificationPermission)
                        }
                    }
                    is NotificationOperationResult.PermissionRequired -> {
                        _uiEvents.tryEmit(UiEvent.RequestNotificationPermission)
                        setNotificationError(NotificationError.PermissionDenied(canRequestAgain = true))
                        _uiState.update { it.copy(isNotificationEnabled = false) }
                    }
                    is NotificationOperationResult.PartialSuccess -> {
                        // Should not happen for single habit operation
                        _uiState.update { state ->
                            state.copy(
                                isNotificationEnabled = true,
                                notificationTime = time,
                                notificationError = null
                            )
                        }
                    }
                }
            } else {
                when (val result = habitNotificationUseCase.disable(habitId)) {
                    is NotificationOperationResult.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                isNotificationEnabled = false,
                                notificationError = null
                            )
                        }
                    }
                    is NotificationOperationResult.Error -> {
                        setNotificationError(result.error)
                    }
                    else -> {
                        // Handle other cases if needed
                    }
                }
            }
        }
    }

    /**
     * Update both notification time and period together to avoid race condition
     * This ensures period is set correctly when time is changed
     */
    fun updateNotificationTimeAndPeriod(time: LocalTime, period: NotificationPeriod) {
        viewModelScope.launch {
            // STEP 1: Check permission/global status FIRST (same as toggleNotification)
            when (globalNotificationUseCase.checkStatus()) {
                is GlobalNotificationUseCase.GlobalStatus.NeedsSystemPermission -> {
                    _uiEvents.tryEmit(UiEvent.RequestNotificationPermission)
                    _uiState.update { it.copy(isNotificationEnabled = false) }
                    return@launch
                }
                is GlobalNotificationUseCase.GlobalStatus.NeedsGlobalEnable -> {
                    _uiEvents.tryEmit(UiEvent.ShowGlobalEnableDialog(_uiState.value.habit?.title))
                    _uiState.update { it.copy(isNotificationEnabled = false) }
                    return@launch
                }
                else -> {
                    // Proceed with update
                }
            }

            // Use atomic update via HabitNotificationUseCase.updateTimeAndPeriod
            when (val result = habitNotificationUseCase.updateTimeAndPeriod(habitId, time, period)) {
                is NotificationOperationResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isNotificationEnabled = true,
                            notificationTime = time,
                            notificationPeriod = period,
                            notificationError = null
                        )
                    }
                }
                is NotificationOperationResult.Error -> {
                    setNotificationError(result.error)
                    _uiState.update { it.copy(isNotificationEnabled = false) }
                }
                else -> {
                    // Handle other cases
                }
            }
        }
    }

    fun openAppSettings() {
        _uiEvents.tryEmit(UiEvent.OpenAppSettings)
    }

    private fun setNotificationError(error: NotificationError) {
        _uiState.update { it.copy(notificationError = error) }
    }

    fun clearNotificationError() {
        _uiState.update { it.copy(notificationError = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadNotificationPreferences() {
        viewModelScope.launch {
            try {
                val prefs = notificationPreferencesUseCase.get()
                _uiState.update {
                    it.copy(
                        notificationSoundEnabled = prefs.soundEnabled,
                        notificationVibrationEnabled = prefs.vibrationEnabled
                    )
                }
            } catch (e: Exception) {
                // Use defaults if preferences can't be loaded
            }
        }
    }

    fun updateNotificationSound(enabled: Boolean) {
        _uiState.update { it.copy(notificationSoundEnabled = enabled) }
    }

    fun updateNotificationVibration(enabled: Boolean) {
        _uiState.update { it.copy(notificationVibrationEnabled = enabled) }
    }

    fun enableGlobalNotifications() {
        viewModelScope.launch {
            // Save preferences first
            try {
                notificationPreferencesUseCase.updateBoth(
                    soundEnabled = _uiState.value.notificationSoundEnabled,
                    vibrationEnabled = _uiState.value.notificationVibrationEnabled
                )
            } catch (e: Exception) {
            }

            // Then enable global notifications
            when (val result = globalNotificationUseCase.enable()) {
                is NotificationOperationResult.Success,
                is NotificationOperationResult.PartialSuccess -> {
                    // Now try to enable the habit notification
                    toggleNotification(true)
                }
                is NotificationOperationResult.Error -> {
                    setNotificationError(NotificationError.GeneralError(result.error))
                }
                is NotificationOperationResult.PermissionRequired -> {
                    setNotificationError(NotificationError.PermissionDenied(canRequestAgain = true))
                }
            }
        }
    }


    fun onPermissionGranted() {
        viewModelScope.launch {
            val time = _uiState.value.notificationTime ?: LocalTime(9, 0)
            when (val result = habitNotificationUseCase.enable(habitId, time)) {
                is NotificationOperationResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isNotificationEnabled = true,
                            notificationTime = time,
                            notificationError = null
                        )
                    }
                }
                is NotificationOperationResult.Error -> {
                    setNotificationError(result.error)
                    _uiState.update { it.copy(isNotificationEnabled = false) }
                }
                else -> {
                    // Handle other cases
                }
            }
        }
    }

    fun onPermissionDenied(canRequestAgain: Boolean) {
        setNotificationError(NotificationError.PermissionDenied(canRequestAgain))
        _uiState.update { it.copy(isNotificationEnabled = false) }
    }

    private fun observeGlobalNotificationStatus() {
        viewModelScope.launch {
            preferencesRepository.isNotificationsEnabled().collect { enabled ->
                _uiState.update { it.copy(isGlobalNotificationEnabled = enabled) }
            }
        }
    }

    fun updateNotificationPeriod(period: NotificationPeriod) {
        viewModelScope.launch {
            when (val result = habitNotificationUseCase.updatePeriod(habitId, period)) {
                is NotificationOperationResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            notificationPeriod = period,
                            notificationError = null
                        )
                    }
                }
                is NotificationOperationResult.Error -> {
                    setNotificationError(result.error)
                }
                else -> {
                }
            }
        }
    }

    data class HabitStats(
        val currentStreak: Int = 0,
        val longestStreak: Int = 0,
        val totalDays: Int = 0,
        val completionRate: Double = 0.0,
        val averagePerDay: Double = 0.0,
        val thisWeekCount: Int = 0,
        val thisMonthCount: Int = 0,
        val lastCompleted: LocalDate? = null
    )

    sealed class UiEvent {
        data object RequestNotificationPermission : UiEvent()
        data object OpenAppSettings : UiEvent()
        data class ShowGlobalEnableDialog(val habitName: String?) : UiEvent()
    }
}