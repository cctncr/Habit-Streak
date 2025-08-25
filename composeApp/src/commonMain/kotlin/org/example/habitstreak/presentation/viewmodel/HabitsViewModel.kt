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
import kotlinx.datetime.LocalDate
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

                // Load streaks for each habit
                habits.forEach { habitWithCompletion ->
                    loadStreakForHabit(habitWithCompletion.habit.id)
                }
            }
        }
    }

    private fun loadStreakForHabit(habitId: String) {
        viewModelScope.launch {
            calculateStreakUseCase(habitId).fold(
                onSuccess = { streakInfo ->
                    _uiState.update { state ->
                        state.copy(
                            streaks = state.streaks + (habitId to streakInfo.currentStreak)
                        )
                    }
                },
                onFailure = { error ->
                    // Log error but don't crash the app
                    println("Failed to calculate streak for habit $habitId: ${error.message}")
                }
            )
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
                    loadStreakForHabit(habitId)
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