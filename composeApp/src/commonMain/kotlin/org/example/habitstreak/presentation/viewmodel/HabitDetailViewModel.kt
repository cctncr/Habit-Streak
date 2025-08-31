package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.example.habitstreak.presentation.screen.habit_detail.StatsTimeFilter
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
import kotlin.collections.filter

@OptIn(ExperimentalTime::class)
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
                selectedMonth = org.example.habitstreak.presentation.model.YearMonth(
                    today.year,
                    today.monthNumber
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
            habitRecordRepository.observeRecordsForHabit(habitId).collect { records: List<HabitRecord> ->
                _uiState.update { it.copy(records = records, isLoading = false) }
                calculateStatistics()
            }
        }
    }

    private fun observeNotificationConfig() {
        if (notificationService == null) {
            // Fallback to habit's reminderTime
            viewModelScope.launch {
                habitRepository.observeHabitById(habitId).collect { habit: Habit? ->
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
        if (time != null && notificationService != null) {
            viewModelScope.launch {
                enableNotification(time)
            }
        } else {
            _uiState.update { it.copy(notificationTime = time) }
        }
    }

    private suspend fun enableNotification(time: LocalTime) {
        notificationService?.enableNotification(habitId, time)?.fold(
            onSuccess = {
                _uiState.update { state ->
                    state.copy(
                        isNotificationEnabled = true,
                        notificationTime = time,
                        error = null
                    )
                }
            },
            onFailure = { error: Throwable ->
                _uiState.update { it.copy(error = error.message) }
            }
        )
    }

    private suspend fun disableNotification() {
        notificationService?.disableNotification(habitId)?.fold(
            onSuccess = {
                _uiState.update { state ->
                    state.copy(
                        isNotificationEnabled = false,
                        error = null
                    )
                }
            },
            onFailure = { error: Throwable ->
                _uiState.update { it.copy(error = error.message) }
            }
        )
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
                    calculateStatistics()
                },
                onFailure = { error: Throwable ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    private fun calculateStatistics() {
        viewModelScope.launch {
            val habit = _uiState.value.habit ?: return@launch
            val records = _uiState.value.records

            if (records.isEmpty()) {
                _uiState.update { it.copy(statistics = null, filteredStatistics = null) }
                return@launch
            }

            calculateStreakUseCase(habitId).fold(
                onSuccess = { streakInfo: CalculateStreakUseCase.StreakInfo ->
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
                onFailure = { error: Throwable ->
                    _uiState.update { it.copy(error = "Failed to calculate statistics") }
                }
            )
        }
    }

    private fun calculateCompletionRate(records: List<HabitRecord>, habit: Habit): Double {
        val completedDays = records.count { it.completedCount >= habit.targetCount }
        val createdDate = habit.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val today = dateProvider.today()
        val daysSinceCreation = (today.toEpochDays() - createdDate.toEpochDays()).toInt() + 1
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

    private fun calculateFilteredStats(records: List<HabitRecord>, today: LocalDate, filter: StatsTimeFilter): HabitStats {
        val filteredRecords = when (filter) {
            StatsTimeFilter.ALL_TIME -> records
            StatsTimeFilter.THIS_YEAR -> {
                val yearStart = LocalDate(today.year, 1, 1)
                records.filter { it.date >= yearStart }
            }
            StatsTimeFilter.THIS_MONTH -> {
                val monthStart = LocalDate(today.year, today.month, 1)
                records.filter { it.date >= monthStart }
            }
            StatsTimeFilter.THIS_WEEK -> {
                val weekStart = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))
                records.filter { it.date >= weekStart }
            }
            StatsTimeFilter.LAST_30_DAYS -> {
                val thirtyDaysAgo = today.minus(DatePeriod(days = 30))
                records.filter { it.date >= thirtyDaysAgo }
            }
        }

        return calculateStatsFromRecords(filteredRecords, today)
    }

    private fun calculateStatsFromRecords(records: List<HabitRecord>, today: LocalDate): HabitStats {
        if (records.isEmpty()) return HabitStats()

        val habit = _uiState.value.habit ?: return HabitStats()
        val completedDates = records.filter { it.completedCount >= habit.targetCount }.map { it.date }
        val sortedDates = completedDates.sorted()

        val currentStreak = calculateCurrentStreakFromDates(sortedDates, today)
        val longestStreak = calculateBestStreakForPeriod(sortedDates)
        val totalDays = records.size
        val completionRate = if (totalDays > 0) completedDates.size.toDouble() / totalDays else 0.0
        val averagePerDay = if (totalDays > 0) records.sumOf { it.completedCount }.toDouble() / totalDays else 0.0

        val thisWeekStart = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))
        val thisWeekCount = records.filter { it.date >= thisWeekStart && it.date <= today }.sumOf { it.completedCount }

        val thisMonthStart = LocalDate(today.year, today.month, 1)
        val thisMonthCount = records.filter { it.date >= thisMonthStart }.sumOf { it.completedCount }

        val lastCompleted = completedDates.maxOrNull()

        return HabitStats(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalDays = totalDays,
            completionRate = completionRate,
            averagePerDay = averagePerDay,
            thisWeekCount = thisWeekCount,
            thisMonthCount = thisMonthCount,
            lastCompleted = lastCompleted
        )
    }

    private fun calculateCurrentStreakFromDates(sortedDates: List<LocalDate>, today: LocalDate): Int {
        if (sortedDates.isEmpty()) return 0

        val lastDate = sortedDates.last()
        val daysSinceLastCompletion = today.toEpochDays() - lastDate.toEpochDays()

        // If last completion is not today or yesterday, streak is 0
        if (daysSinceLastCompletion > 1L) return 0

        // Count consecutive days backwards from the latest completion
        var streak = 1
        for (i in sortedDates.size - 2 downTo 0) {
            val daysDiff = sortedDates[i + 1].toEpochDays() - sortedDates[i].toEpochDays()
            if (daysDiff == 1L) {
                streak++
            } else {
                break
            }
        }

        return streak
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

    fun changeMonth(yearMonth: org.example.habitstreak.presentation.model.YearMonth) {
        _uiState.update { it.copy(selectedMonth = yearMonth) }
    }

    fun updateStatsFilter(filter: StatsTimeFilter) {
        val today = dateProvider.today()
        val filteredStats = calculateFilteredStats(_uiState.value.records, today, filter)
        _uiState.update {
            it.copy(filteredStatistics = filteredStats)
        }
    }

    fun deleteHabit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Cancel notifications before deleting (null-safe)
            notificationService?.disableNotification(habitId)

            habitRepository.deleteHabit(habitId).fold(
                onSuccess = { onSuccess() },
                onFailure = { error: Throwable ->
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
}