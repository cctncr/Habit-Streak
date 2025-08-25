package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.repository.StatisticsRepository
import org.example.habitstreak.presentation.ui.state.StatisticsUiState

class StatisticsViewModel(
    private val statisticsRepository: StatisticsRepository,
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            statisticsRepository.getAllStatistics().fold(
                onSuccess = { statistics ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            statistics = statistics
                        )
                    }
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
        }
    }

    fun selectHabit(habitId: String) {
        viewModelScope.launch {
            statisticsRepository.getHabitStatistics(habitId).fold(
                onSuccess = { stats ->
                    _uiState.update { state ->
                        state.copy(selectedHabitStats = stats)
                    }
                },
                onFailure = { /* Handle error */ }
            )
        }
    }
}