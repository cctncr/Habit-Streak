package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.usecase.ArchiveHabitUseCase
import org.example.habitstreak.domain.usecase.CalculateStreakUseCase
import org.example.habitstreak.domain.usecase.GetHabitsWithCompletionUseCase
import org.example.habitstreak.domain.usecase.ToggleHabitCompletionUseCase
import org.example.habitstreak.presentation.ui.state.HabitsUiState
import org.example.habitstreak.domain.util.DateProvider

class HabitsViewModel(
    private val getHabitsWithCompletionUseCase: GetHabitsWithCompletionUseCase,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val archiveHabitUseCase: ArchiveHabitUseCase,
    private val habitRecordRepository: HabitRecordRepository,
    private val dateProvider: DateProvider
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(dateProvider.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    // Habit-specific loading states
    private val _habitLoadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val habitsWithCompletion = _selectedDate.flatMapLatest { date ->
        getHabitsWithCompletionUseCase(date)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            habitsWithCompletion.collect { habits ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        habits = habits
                    )
                }

                // Load streaks and histories for all habits (sadece bir kez)
                habits.forEach { habitWithCompletion ->
                    loadHabitDataSilently(habitWithCompletion.habit.id)
                }
            }
        }
    }

    private fun loadHabitDataSilently(habitId: String) {
        viewModelScope.launch {
            // Streak hesaplama
            calculateStreakUseCase(habitId).fold(
                onSuccess = { streakInfo ->
                    _uiState.update { state ->
                        state.copy(
                            streaks = state.streaks + (habitId to streakInfo.currentStreak)
                        )
                    }
                },
                onFailure = { /* Sessizce devam et */ }
            )

            // Completion history yükleme (son 1 yıl)
            val today = dateProvider.today()
            val startDate = today.minus(DatePeriod(days = 364))

            habitRecordRepository.getRecordsBetweenDates(startDate, today).fold(
                onSuccess = { records ->
                    val habitRecords = records.filter { it.habitId == habitId }
                    val habitInfo = habitsWithCompletion.value.find { it.habit.id == habitId }?.habit
                    val targetCount = habitInfo?.targetCount ?: 1

                    val completionMap = habitRecords.associate { record ->
                        record.date to (record.completedCount.toFloat() / targetCount.coerceAtLeast(1))
                    }

                    _uiState.update { state ->
                        state.copy(
                            completionHistories = state.completionHistories +
                                    (habitId to completionMap)
                        )
                    }
                },
                onFailure = { /* Sessizce devam et */ }
            )
        }
    }

    fun updateHabitProgress(habitId: String, date: LocalDate, value: Int) {
        viewModelScope.launch {
            // GLOBAL loading state KULLANMA - Bu tüm kartları etkiler

            if (value == 0) {
                habitRecordRepository.markHabitAsIncomplete(habitId, date).fold(
                    onSuccess = { updateSingleHabitData(habitId) },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
                )
            } else {
                habitRecordRepository.markHabitAsComplete(habitId, date, value).fold(
                    onSuccess = { updateSingleHabitData(habitId) },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
                )
            }
        }
    }

    private suspend fun updateSingleHabitData(habitId: String) {
        // Batch update - Tek seferde tüm değişiklikleri yap
        val today = dateProvider.today()
        val startDate = today.minus(DatePeriod(days = 364))

        // Streak ve history'yi paralel olarak al
        val streakResult = calculateStreakUseCase(habitId)
        val historyResult = habitRecordRepository.getRecordsBetweenDates(startDate, today)

        // Sonuçları tek update'te uygula
        _uiState.update { state ->
            var newStreaks = state.streaks
            var newHistories = state.completionHistories

            // Streak update
            streakResult.fold(
                onSuccess = { streakInfo ->
                    newStreaks = newStreaks + (habitId to streakInfo.currentStreak)
                },
                onFailure = { /* Ignore */ }
            )

            // History update
            historyResult.fold(
                onSuccess = { records ->
                    val habitRecords = records.filter { it.habitId == habitId }
                    val habitInfo = habitsWithCompletion.value.find { it.habit.id == habitId }?.habit
                    val targetCount = habitInfo?.targetCount ?: 1

                    val completionMap = habitRecords.associate { record ->
                        record.date to (record.completedCount.toFloat() / targetCount.coerceAtLeast(1))
                    }

                    newHistories = newHistories + (habitId to completionMap)
                },
                onFailure = { /* Ignore */ }
            )

            state.copy(
                streaks = newStreaks,
                completionHistories = newHistories
            )
        }
    }

    fun toggleHabitCompletion(habitId: String) {
        viewModelScope.launch {
            // Sadece bu habit için loading
            _habitLoadingStates.update { it + (habitId to true) }

            toggleHabitCompletionUseCase(
                ToggleHabitCompletionUseCase.Params(
                    habitId = habitId,
                    date = _selectedDate.value
                )
            ).fold(
                onSuccess = {
                    updateSingleHabitData(habitId)
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(error = error.message ?: "An error occurred")
                    }
                }
            )

            _habitLoadingStates.update { it - habitId }
        }
    }

    fun archiveHabit(habitId: String) {
        viewModelScope.launch {
            archiveHabitUseCase(
                ArchiveHabitUseCase.Params(habitId, true)
            ).fold(
                onSuccess = { /* Flow automatically handles UI update */ },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(error = error.message ?: "Failed to archive habit")
                    }
                }
            )
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Habit-specific loading durumunu kontrol etmek için
    fun isHabitLoading(habitId: String): Boolean {
        return _habitLoadingStates.value[habitId] == true
    }
}