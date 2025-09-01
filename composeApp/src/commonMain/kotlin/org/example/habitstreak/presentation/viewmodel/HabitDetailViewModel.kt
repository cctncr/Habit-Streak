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
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.service.PermissionResult
import org.example.habitstreak.domain.usecase.CalculateStreakUseCase
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.presentation.model.YearMonth
import org.example.habitstreak.presentation.ui.state.HabitDetailUiState
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class HabitDetailViewModel(
    private val habitId: String,
    private val habitRepository: HabitRepository,
    private val habitRecordRepository: HabitRecordRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val dateProvider: DateProvider,
    private val notificationService: NotificationService? = null,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    init {
        loadData()
        observeNotificationConfig()
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

            val today = dateProvider.today()
            val fullyCompletedDates = records.filter { record ->
                record.completedCount >= habit.targetCount.coerceAtLeast(1)
            }.map { it.date }

            // Calculate streaks
            val streakResult = calculateStreakUseCase(habitId)

            val streakInfo = streakResult.getOrNull()

            // Calculate completion rate
            val last30Days = (0..29).map { today.minus(DatePeriod(days = it)) }
            val completedInLast30 = last30Days.count { it in fullyCompletedDates }
            val completionRate = (completedInLast30 / 30.0) * 100

            // Calculate this week and month stats
            val weekStart = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))
            val monthStart = LocalDate(today.year, today.monthNumber, 1)

            val thisWeekCount = fullyCompletedDates.count {
                it >= weekStart && it <= today
            }

            val thisMonthCount = fullyCompletedDates.count {
                it >= monthStart && it <= today
            }

            val averagePerDay = if (fullyCompletedDates.isNotEmpty()) {
                val daysSinceFirstRecord = records.minOfOrNull { it.date }?.let { firstDate ->
                    (today.toEpochDays() - firstDate.toEpochDays()).toInt() + 1
                } ?: 1
                fullyCompletedDates.size.toDouble() / daysSinceFirstRecord
            } else 0.0

            _uiState.update { state ->
                state.copy(
                    stats = HabitStats(
                        currentStreak = streakInfo?.currentStreak ?: 0,
                        longestStreak = streakInfo?.longestStreak ?: 0,
                        totalDays = fullyCompletedDates.size,
                        completionRate = completionRate,
                        averagePerDay = averagePerDay,
                        thisWeekCount = thisWeekCount,
                        thisMonthCount = thisMonthCount,
                        lastCompleted = fullyCompletedDates.maxOrNull()
                    )
                )
            }
        }
    }

    fun changeMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(selectedMonth = yearMonth) }
    }

    fun updateProgress(date: LocalDate, completedCount: Int, note: String? = null) {
        viewModelScope.launch {
            if (completedCount == 0) {
                habitRecordRepository.markHabitAsIncomplete(habitId, date).fold(
                    onSuccess = {
                        calculateStatistics()
                    },
                    onFailure = { error: Throwable ->
                        _uiState.update { it.copy(error = error.message) }
                    }
                )
            } else {
                habitRecordRepository.markHabitAsComplete(
                    habitId = habitId,
                    date = date,
                    count = completedCount,
                    note = note ?: ""
                ).fold(
                    onSuccess = {
                        calculateStatistics()
                    },
                    onFailure = { error: Throwable ->
                        _uiState.update { it.copy(error = error.message) }
                    }
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
        if (notificationService == null) {
            viewModelScope.launch {
                habitRepository.observeHabitById(habitId).collect { habit ->
                    val currentError = _uiState.value.notificationError
                    _uiState.update { state ->
                        state.copy(
                            isNotificationEnabled = habit?.isReminderEnabled == true,
                            notificationTime = habit?.reminderTime?.let {
                                try {
                                    LocalTime.parse(it)
                                } catch (e: Exception) {
                                    null
                                }
                            },
                            notificationError = if (currentError?.shouldPreserve() == true) currentError else null
                        )
                    }
                }
            }
            return
        }

        viewModelScope.launch {
            notificationService.observeNotificationConfig(habitId).collect { config ->
                val currentError = _uiState.value.notificationError
                _uiState.update { state ->
                    state.copy(
                        notificationConfig = config,
                        isNotificationEnabled = config?.isEnabled == true,
                        notificationTime = config?.time,
                        notificationError = if (currentError?.shouldPreserve() == true) currentError else null
                    )
                }
            }
        }
    }

    fun toggleNotification(enabled: Boolean) {
        if (notificationService == null) {
            setNotificationError(NotificationError.ServiceUnavailable())
            return
        }

        viewModelScope.launch {
            if (enabled) {
                val permissionStatus = notificationService.checkPermissionStatus()

                when (permissionStatus) {
                    is PermissionResult.Granted -> {
                        val time = _uiState.value.notificationTime ?: LocalTime(9, 0)
                        enableNotification(time)
                    }
                    is PermissionResult.DeniedCanAskAgain -> {
                        _uiEvents.tryEmit(UiEvent.RequestNotificationPermission)
                        setNotificationError(NotificationError.PermissionDenied(canRequestAgain = true))
                        _uiState.update { it.copy(isNotificationEnabled = false) }
                    }
                    is PermissionResult.DeniedPermanently -> {
                        setNotificationError(NotificationError.PermissionDenied(canRequestAgain = false))
                        _uiState.update { it.copy(isNotificationEnabled = false) }
                    }
                    is PermissionResult.Error -> {
                        setNotificationError(permissionStatus.error)
                        _uiState.update { it.copy(isNotificationEnabled = false) }
                    }
                }
            } else {
                disableNotification()
            }
        }
    }

    fun updateNotificationTime(time: LocalTime?) {
        if (time == null) {
            _uiState.update { it.copy(notificationTime = time) }
            return
        }

        if (notificationService == null) {
            setNotificationError(NotificationError.ServiceUnavailable())
            return
        }

        viewModelScope.launch {
            if (!_uiState.value.isNotificationEnabled) {
                toggleNotification(true)
            } else {
                enableNotification(time)
            }
        }
    }

    private suspend fun enableNotification(time: LocalTime) {
        notificationService?.enableNotification(habitId, time)?.fold(
            onSuccess = {
                _uiState.update { state ->
                    state.copy(
                        isNotificationEnabled = true,
                        notificationTime = time,
                        notificationError = null
                    )
                }
            },
            onFailure = { error: Throwable ->
                val notificationError = when (error) {
                    is NotificationError -> error
                    else -> NotificationError.GeneralError(error)
                }

                if (notificationError is NotificationError.PermissionDenied && notificationError.canRequestAgain) {
                    _uiEvents.tryEmit(UiEvent.RequestNotificationPermission)
                }

                setNotificationError(notificationError)
                _uiState.update { it.copy(isNotificationEnabled = false) }
            }
        )
    }

    private suspend fun disableNotification() {
        notificationService?.disableNotification(habitId)?.fold(
            onSuccess = {
                _uiState.update { state ->
                    state.copy(
                        isNotificationEnabled = false,
                        notificationError = null
                    )
                }
            },
            onFailure = { error: Throwable ->
                val notificationError = when (error) {
                    is NotificationError -> error
                    else -> NotificationError.GeneralError(error)
                }
                setNotificationError(notificationError)
            }
        )
    }

    fun openAppSettings() {
        viewModelScope.launch {
            val success = notificationService?.openAppSettings() ?: false

            if (!success) {
                _uiEvents.tryEmit(UiEvent.OpenAppSettings)
            }
        }
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

    fun onPermissionGranted() {
        viewModelScope.launch {
            val time = _uiState.value.notificationTime ?: LocalTime(9, 0)
            enableNotification(time)
        }
    }

    fun onPermissionDenied(canRequestAgain: Boolean) {
        setNotificationError(NotificationError.PermissionDenied(canRequestAgain))
        _uiState.update { it.copy(isNotificationEnabled = false) }
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
        object RequestNotificationPermission : UiEvent()
        object OpenAppSettings : UiEvent()
    }
}