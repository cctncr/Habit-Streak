package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.example.habitstreak.presentation.screen.habit_detail.StatsTimeFilter
import org.example.habitstreak.presentation.ui.utils.isInCurrentWeekFromMonday
import org.example.habitstreak.presentation.ui.utils.startOfWeekFromMonday
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import org.example.habitstreak.presentation.ui.state.HabitDetailUiState
import kotlin.time.ExperimentalTime
import androidx.lifecycle.ViewModel
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.service.NotificationService
import org.example.habitstreak.domain.usecase.CalculateStreakUseCase
import org.example.habitstreak.domain.util.DateProvider

class HabitDetailViewModel(
    private val habitId: String,
    private val habitRepository: HabitRepository,
    private val habitRecordRepository: HabitRecordRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val dateProvider: DateProvider,
    private val notificationService: NotificationService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeNotificationConfig()
    }

    fun loadData() {
        val today = dateProvider.today()

        _uiState.update {
            it.copy(
                selectedMonth = YearMonth(today.year, today.month.number),
                isLoading = true
            )
        }

        loadHabitData()
        loadRecords()
    }

    private fun loadHabitData() {
        viewModelScope.launch {
            habitRepository.observeHabitById(habitId).collect { habit ->
                _uiState.update { it.copy(habit = habit) }
                if (habit != null) {
                    calculateStatistics()
                }
            }
        }
    }

    private fun observeNotificationConfig() {
        if (notificationService == null) {
            // Fallback to habit's reminderTime
            viewModelScope.launch {
                habitRepository.observeHabitById(habitId).collect { habit ->
                    _uiState.update { state ->
                        state.copy(
                            isNotificationEnabled = habit?.isReminderEnabled == true,
                            notificationTime = habit?.reminderTime?.let {
                                try {
                                    LocalTime.parse(it)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        )
                    }
                }
            }
            return
        }

        viewModelScope.launch {
            notificationService.observeNotificationConfig(habitId).collect { config ->
                _uiState.update { state ->
                    state.copy(
                        notificationConfig = config,
                        isNotificationEnabled = config?.isEnabled == true,
                        notificationTime = config?.time
                    )
                }
            }
        }
    }

    fun toggleNotification(enabled: Boolean) {
        if (notificationService == null) {
            _uiState.update { it.copy(error = "Notification service not available") }
            return
        }

        viewModelScope.launch {
            if (enabled) {
                val time = _uiState.value.notificationTime ?: LocalTime(9, 0)
                enableNotification(time)
            } else {
                disableNotification()
            }
        }
    }

    fun updateNotificationTime(time: LocalTime?) {
        if (notificationService == null) {
            _uiState.update { it.copy(error = "Notification service not available") }
            return
        }

        if (time == null) {
            disableNotification()
            return
        }

        viewModelScope.launch {
            val result = if (_uiState.value.isNotificationEnabled) {
                notificationService.updateNotificationTime(habitId, time)
            } else {
                enableNotification(time)
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(notificationTime = time, error = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    private fun enableNotification(time: LocalTime) {
        if (notificationService == null) return

        viewModelScope.launch {
            val habit = _uiState.value.habit ?: return@launch
            val message = "Time to ${habit.title}"

            notificationService.enableNotification(
                habitId = habitId,
                time = time,
                message = message
            ).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isNotificationEnabled = true,
                            notificationTime = time,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    private fun disableNotification() {
        if (notificationService == null) return

        viewModelScope.launch {
            notificationService.disableNotification(habitId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isNotificationEnabled = false,
                            notificationTime = null,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun updateProgress(date: LocalDate, value: Int) {
        viewModelScope.launch {
            habitRecordRepository.markHabitAsComplete(habitId, date, value, "")
            loadHabitData()
        }
    }

    fun updateNote(date: LocalDate, note: String) {
        viewModelScope.launch {
            val record = _uiState.value.records.find { it.date == date }
            if (record != null) {
                habitRecordRepository.markHabitAsComplete(
                    habitId,
                    date,
                    record.completedCount,
                    note
                )
                loadHabitData()
            }
        }
    }

    // Açık bir reload için ayrı fonksiyon
    fun reloadData() {
        isDataLoaded = false
        loadData()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            habitRecordRepository.observeRecordsForHabit(habitId).collect { records ->
                _uiState.update { it.copy(
                    records = records,
                    isLoading = false
                )}
                calculateStatistics()
            }
        }
    }

    fun setFutureReminder(date: LocalDate, time: LocalTime) {
        // Implement future reminder logic if needed
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    private fun calculateStatistics() {
        viewModelScope.launch {
            val habit = _uiState.value.habit ?: return@launch
            val records = _uiState.value.records

            if (records.isEmpty()) {
                _uiState.update { it.copy(statistics = null, filteredStatistics = null) }
                return@launch
            }

            // Calculate stats using the streak use case
            calculateStreakUseCase(habitId).fold(
                onSuccess = { streakInfo ->
                    val stats = HabitStats(
                        currentStreak = streakInfo.currentStreak,
                        longestStreak = streakInfo.longestStreak,
                        totalDays = records.count { it.completedCount >= habit.targetCount },
                        completionRate = calculateCompletionRate(records, habit),
                        averagePerDay = calculateAveragePerDay(records),
                        thisWeekCount = calculateThisWeekCount(records),
                        thisMonthCount = calculateThisMonthCount(records),
                        lastCompleted = streakInfo.lastCompletedDate
                    )
                    _uiState.update {
                        it.copy(
                            statistics = stats,
                            filteredStatistics = stats
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(error = "Failed to calculate statistics") }
                }
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun calculateCompletionRate(records: List<HabitRecord>, habit: Habit): Double {
        val completedDays = records.count { it.completedCount >= habit.targetCount }
        val createdDate = habit.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val daysSinceCreation = (dateProvider.today().toEpochDays() - createdDate.toEpochDays()).toInt() + 1
        return if (daysSinceCreation > 0) {
            (completedDays.toDouble() / daysSinceCreation) * 100
        } else 0.0
    }

    private fun calculateAveragePerDay(records: List<HabitRecord>): Double {
        val totalCount = records.sumOf { it.completedCount }
        val completedDays = records.filter { it.completedCount > 0 }.size
        return if (completedDays > 0) {
            totalCount.toDouble() / completedDays
        } else 0.0
    }

    private fun calculateThisWeekCount(records: List<HabitRecord>): Int {
        val today = dateProvider.today()
        val weekStart = today.minus((today.dayOfWeek.ordinal + 6) % 7, DateTimeUnit.DAY)
        return records.filter { it.date >= weekStart }.sumOf { it.completedCount }
    }

    private fun calculateThisMonthCount(records: List<HabitRecord>): Int {
        val today = dateProvider.today()
        val monthStart = LocalDate(today.year, today.month, 1)
        return records.filter { it.date >= monthStart }.sumOf { it.completedCount }
    }

    @OptIn(ExperimentalTime::class)
    fun updateStatsFilter(filter: StatsTimeFilter) {
        currentFilter = filter
        viewModelScope.launch {
            val habit = _uiState.value.habit ?: return@launch
            val records = _uiState.value.records
            val today = dateProvider.today()
            val currentMonth = _uiState.value.currentMonth

            val filteredRecords = when (filter) {
                StatsTimeFilter.ALL_TIME -> {
                    records.filter { it.date >= habit.createdAt }
                }

                StatsTimeFilter.THIS_MONTH -> {
                    records.filter { it.date.year == currentMonth.year && it.date.monthNumber == currentMonth.month.number }
                }

                StatsTimeFilter.THIS_WEEK -> {
                    records.filter { it.date.isInCurrentWeekFromMonday(today) }
                }
            }

            val totalCompletions = filteredRecords.count {
                it.completedCount >= habit.targetCount
            }

            val periodDays = when (filter) {
                StatsTimeFilter.ALL_TIME -> {
                    (today.toEpochDays() - habit.createdAt.toEpochDays() + 1).toInt()
                }

                StatsTimeFilter.THIS_MONTH -> {
                    val daysInMonth = when (currentMonth.month.number) {
                        1, 3, 5, 7, 8, 10, 12 -> 31
                        4, 6, 9, 11 -> 30
                        2 -> if (currentMonth.isLeapYear) 29 else 28
                        else -> 30
                    }
                    if (currentMonth.year == today.year && currentMonth.month.number == today.month.number) {
                        today.day
                    } else {
                        daysInMonth
                    }
                }

                StatsTimeFilter.THIS_WEEK -> {
                    val startOfWeek = today.startOfWeekFromMonday()
                    (today.toEpochDays() - startOfWeek.toEpochDays() + 1).toInt()
                }
            }

            val completionRate = if (periodDays > 0) {
                (totalCompletions * 100) / periodDays.coerceAtLeast(1)
            } else 0

            val bestStreak = calculateBestStreakForPeriod(filteredRecords.map { it.date }.sorted())

            _uiState.update { state ->
                state.copy(
                    filteredStatistics = FilteredStats(
                        totalCompletions = totalCompletions,
                        completionRate = completionRate.coerceIn(0, 100),
                        bestStreak = bestStreak
                    )
                )
            }
        }
    }

    private fun calculateBestStreakForPeriod(sortedDates: List<LocalDate>): Int {
        if (sortedDates.isEmpty()) return 0

        var maxStreak = 1
        var currentStreak = 1

        for (i in 1 until sortedDates.size) {
            val daysDiff = sortedDates[i].toEpochDays() - sortedDates[i - 1].toEpochDays()
            if (daysDiff == 1L) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }

        return maxStreak
    }

    fun changeMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(currentMonth = yearMonth) }
        if (currentFilter == StatsTimeFilter.THIS_MONTH) {
            updateStatsFilter(StatsTimeFilter.THIS_MONTH)
        }
    }

    fun deleteHabit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Cancel notifications before deleting (null-safe)
            notificationService?.disableNotification(habitId)

            habitRepository.deleteHabit(habitId).fold(
                onSuccess = { onSuccess() },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun setFutureReminder(date: LocalDate, time: LocalTime?) {
        // Future implementation for setting reminders
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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

    data class FilteredStats(
        val totalCompletions: Int,
        val completionRate: Int,
        val bestStreak: Int
    )

    data class YearMonth(val year: Int, val month: Month) {
        constructor(year: Int, monthNumber: Int) : this(
            year = when {
                monthNumber > 12 -> year + ((monthNumber - 1) / 12)
                monthNumber < 1 -> year + ((monthNumber - 12) / 12)
                else -> year
            },
            month = Month(
                when {
                    monthNumber > 12 -> ((monthNumber - 1) % 12) + 1
                    monthNumber < 1 -> 12 + (monthNumber % 12)
                    else -> monthNumber
                }
            )
        )

        val isLeapYear: Boolean
            get() = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

        operator fun compareTo(other: YearMonth): Int {
            return when {
                this.year != other.year -> this.year.compareTo(other.year)
                else -> this.month.number.compareTo(other.month.number)
            }
        }

        fun nextMonth(): YearMonth {
            return if (month.number == 12) {
                YearMonth(year + 1, 1)
            } else {
                YearMonth(year, month.number + 1)
            }
        }

        fun previousMonth(): YearMonth {
            return if (month.number == 1) {
                YearMonth(year - 1, 12)
            } else {
                YearMonth(year, month.number - 1)
            }
        }
    }
}