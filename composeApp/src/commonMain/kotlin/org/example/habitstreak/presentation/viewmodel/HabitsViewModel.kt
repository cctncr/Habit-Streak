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

    @OptIn(ExperimentalCoroutinesApi::class)
    val habitsWithCompletion = _selectedDate.flatMapLatest { date ->
        getHabitsWithCompletionUseCase(date)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadHabits()
    }

    private fun loadHabits() {
        viewModelScope.launch {
            habitsWithCompletion.collect { habits ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        habits = habits
                    )
                }

                // Load streaks and completion histories for each habit
                habits.forEach { habitWithCompletion ->
                    loadHabitData(habitWithCompletion.habit.id)
                }
            }
        }
    }

    private fun loadHabitData(habitId: String) {
        viewModelScope.launch {
            // Load streak
            calculateStreakUseCase(habitId).fold(
                onSuccess = { streakInfo ->
                    _uiState.update { state ->
                        state.copy(
                            streaks = state.streaks + (habitId to streakInfo.currentStreak)
                        )
                    }
                },
                onFailure = { error ->
                    println("Failed to calculate streak for habit $habitId: ${error.message}")
                }
            )

            // Load completion history (last 350 days)
            val today = dateProvider.today()
            val startDate = today.minus(DatePeriod(days = 350))

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
                onFailure = { error ->
                    println("Failed to load history for habit $habitId: ${error.message}")
                }
            )
        }
    }

    fun updateHabitProgress(habitId: String, date: LocalDate, value: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            if (value == 0) {
                // Remove completion
                habitRecordRepository.markHabitAsIncomplete(habitId, date).fold(
                    onSuccess = {
                        loadHabitData(habitId)
                        _uiState.update { it.copy(isLoading = false) }
                    },
                    onFailure = { error ->
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to update progress"
                            )
                        }
                    }
                )
            } else {
                // Add or update completion
                habitRecordRepository.markHabitAsComplete(habitId, date, value).fold(
                    onSuccess = {
                        loadHabitData(habitId)
                        _uiState.update { it.copy(isLoading = false) }
                    },
                    onFailure = { error ->
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to update progress"
                            )
                        }
                    }
                )
            }
        }
    }

    fun toggleHabitCompletion(habitId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            toggleHabitCompletionUseCase(
                ToggleHabitCompletionUseCase.Params(
                    habitId = habitId,
                    date = _selectedDate.value
                )
            ).fold(
                onSuccess = {
                    loadHabitData(habitId)
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = error.message ?: "An error occurred"
                        )
                    }
                }
            )
        }
    }

    fun archiveHabit(habitId: String) {
        viewModelScope.launch {
            archiveHabitUseCase(
                ArchiveHabitUseCase.Params(habitId, true)
            ).fold(
                onSuccess = { /* Handled by flow */ },
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
}