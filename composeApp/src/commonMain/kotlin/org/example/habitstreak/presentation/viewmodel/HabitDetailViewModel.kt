package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.usecase.CalculateStreakUseCase
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.presentation.ui.utils.isInCurrentMonth
import org.example.habitstreak.presentation.ui.utils.isInCurrentWeek
import org.example.habitstreak.presentation.screen.habit_detail.StatsTimeFilter
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class HabitDetailViewModel(
    private val habitId: String,
    private val habitRepository: HabitRepository,
    private val habitRecordRepository: HabitRecordRepository,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val dateProvider: DateProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitDetailUiState())
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    private var currentFilter = StatsTimeFilter.ALL_TIME
    private var isDataLoaded = false

    init {
        loadData()
    }

    fun loadData() {
        val today = dateProvider.today() // DateProvider kullanımı

        // Sadece ilk yüklenmede veya açık bir reload talep edildiğinde state'i resetle
        if (!isDataLoaded) {
            _uiState.update {
                it.copy(
                    currentMonth = YearMonth(today.year, today.monthNumber), // DateProvider'dan gelen today
                    isLoading = true
                )
            }
        }

        loadHabitData()
        loadRecords()
        isDataLoaded = true
    }

    // Açık bir reload için ayrı fonksiyon
    fun reloadData() {
        isDataLoaded = false
        loadData()
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

    private fun loadRecords() {
        viewModelScope.launch {
            habitRecordRepository.observeRecordsForHabit(habitId).collect { records ->
                _uiState.update { it.copy(
                    records = records,
                    recentRecords = records.sortedByDescending { it.date },
                    isLoading = false
                )}
                calculateStatistics()
            }
        }
    }

    fun updateNote(date: LocalDate, note: String) {
        viewModelScope.launch {
            val existingNote = _uiState.value.records.find { it.date == date }?.note ?: ""
            if (existingNote == note) return@launch

            habitRecordRepository.updateRecordNote(habitId, date, note).fold(
                onSuccess = {
                    // Başarılı - Flow otomatik günceller
                },
                onFailure = { error ->
                    val existingRecord = _uiState.value.records.find { it.date == date }
                    val progress = existingRecord?.completedCount ?: 0

                    habitRecordRepository.markHabitAsComplete(
                        habitId = habitId,
                        date = date,
                        count = progress,
                        note = note
                    ).fold(
                        onSuccess = {
                            // Note başarıyla kaydedildi
                        },
                        onFailure = { fallbackError ->
                            _uiState.update { it.copy(error = fallbackError.message) }
                        }
                    )
                }
            )
        }
    }

    fun updateProgress(date: LocalDate, value: Int) {
        viewModelScope.launch {
            val existingRecord = _uiState.value.records.find { it.date == date }
            val existingNote = existingRecord?.note ?: ""

            if (value == 0) {
                if (existingNote.isNotBlank()) {
                    habitRecordRepository.markHabitAsComplete(habitId, date, 0, existingNote)
                } else {
                    habitRecordRepository.markHabitAsIncomplete(habitId, date)
                }
            } else {
                habitRecordRepository.markHabitAsComplete(habitId, date, value, existingNote).fold(
                    onSuccess = {
                        // Başarılı
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
                )
            }
        }
    }

    private fun calculateStatistics() {
        viewModelScope.launch {
            val habit = _uiState.value.habit ?: return@launch
            val records = _uiState.value.records
            val today = dateProvider.today() // DateProvider kullanımı

            calculateStreakUseCase(habitId).fold(
                onSuccess = { streakInfo ->
                    val thisMonthCompletions = records.count { record ->
                        record.date.isInCurrentMonth(today) &&
                                record.completedCount >= habit.targetCount
                    }

                    val totalCompletions = records.count {
                        it.completedCount >= habit.targetCount
                    }

                    val thirtyDaysAgo = today.minus(DatePeriod(days = 30))
                    val recentRecords = records.filter { it.date >= thirtyDaysAgo }
                    val daysWithProgress = recentRecords.count {
                        it.completedCount >= habit.targetCount
                    }
                    val completionRate = if (daysWithProgress > 0) {
                        (daysWithProgress * 100) / 30
                    } else 0

                    _uiState.update { state ->
                        state.copy(
                            statistics = HabitStats(
                                currentStreak = streakInfo.currentStreak,
                                longestStreak = streakInfo.longestStreak,
                                totalCompletions = totalCompletions,
                                completionRate = completionRate,
                                thisMonthCompletions = thisMonthCompletions
                            )
                        )
                    }

                    updateStatsFilter(currentFilter)
                },
                onFailure = { /* Handle error */ }
            )
        }
    }

    fun updateStatsFilter(filter: StatsTimeFilter) {
        currentFilter = filter
        viewModelScope.launch {
            val habit = _uiState.value.habit ?: return@launch
            val records = _uiState.value.records
            val today = dateProvider.today() // DateProvider kullanımı

            val filteredRecords = when (filter) {
                StatsTimeFilter.ALL_TIME -> {
                    records.filter { it.date >= habit.createdAt }
                }
                StatsTimeFilter.THIS_MONTH -> {
                    records.filter { it.date.isInCurrentMonth(today) }
                }
                StatsTimeFilter.THIS_WEEK -> {
                    records.filter { it.date.isInCurrentWeek(today) }
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
                    today.day
                }
                StatsTimeFilter.THIS_WEEK -> {
                    today.dayOfWeek.ordinal + 1
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
    }

    fun deleteHabit(onSuccess: () -> Unit) {
        viewModelScope.launch {
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

    data class HabitDetailUiState(
        val habit: Habit? = null,
        val records: List<HabitRecord> = emptyList(),
        val recentRecords: List<HabitRecord> = emptyList(),
        val statistics: HabitStats? = null,
        val filteredStatistics: FilteredStats? = null,
        val currentMonth: YearMonth = YearMonth(2025, 1), // Default, loadData()'da DateProvider ile güncellenir
        val isLoading: Boolean = false,
        val error: String? = null
    )

    data class HabitStats(
        val currentStreak: Int,
        val longestStreak: Int,
        val totalCompletions: Int,
        val completionRate: Int,
        val thisMonthCompletions: Int
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
            month = Month(when {
                monthNumber > 12 -> ((monthNumber - 1) % 12) + 1
                monthNumber < 1 -> 12 + (monthNumber % 12)
                else -> monthNumber
            })
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