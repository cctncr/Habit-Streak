package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.usecase.habit.RestoreHabitUseCase

class ArchivedHabitsViewModel(
    private val habitRepository: HabitRepository,
    private val restoreHabitUseCase: RestoreHabitUseCase
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val archivedHabits: StateFlow<List<Habit>> = habitRepository.observeArchivedHabits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            archivedHabits.collect {
                _uiState.update { state ->
                    state.copy(isLoading = false)
                }
            }
        }
    }

    fun restoreHabit(habitId: String) {
        viewModelScope.launch {
            restoreHabitUseCase(habitId).fold(
                onSuccess = {
                    _uiState.update { it.copy(error = null) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message ?: "Failed to restore habit") }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
