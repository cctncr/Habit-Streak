package org.example.habit_streak.presentation.viewmodel

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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.habit_streak.domain.usecase.ArchiveHabitUseCase
import org.example.habit_streak.domain.usecase.CalculateStreakUseCase
import org.example.habit_streak.domain.usecase.GetHabitsWithCompletionUseCase
import org.example.habit_streak.domain.usecase.ToggleHabitCompletionUseCase
import org.example.habit_streak.presentation.ui.state.HabitsUiState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class HabitsViewModel(
    private val getHabitsWithCompletionUseCase: GetHabitsWithCompletionUseCase,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val archiveHabitUseCase: ArchiveHabitUseCase
) : ViewModel() {

    @OptIn(ExperimentalTime::class)
    private val _selectedDate = MutableStateFlow(
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date // This would need proper date provider
    )
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
                onFailure = { /* Handle error */ }
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
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
            )

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun archiveHabit(habitId: String) {
        viewModelScope.launch {
            archiveHabitUseCase(
                ArchiveHabitUseCase.Params(habitId, true)
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