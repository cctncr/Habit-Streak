package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.model.HabitColor
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.HabitIcon
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.usecase.CreateHabitUseCase
import org.example.habitstreak.presentation.ui.state.CreateEditHabitUiState
import kotlin.time.ExperimentalTime

class CreateEditHabitViewModel(
    private val createHabitUseCase: CreateHabitUseCase,
    private val habitRepository: HabitRepository,
    private val habitId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditHabitUiState())
    val uiState: StateFlow<CreateEditHabitUiState> = _uiState.asStateFlow()

    init {
        habitId?.let { loadHabit(it) }
    }

    private fun loadHabit(id: String) {
        viewModelScope.launch {
            habitRepository.getHabitById(id).fold(
                onSuccess = { habit ->
                    habit?.let {
                        _uiState.value = CreateEditHabitUiState(
                            title = it.title,
                            description = it.description,
                            selectedIcon = it.icon,
                            selectedColor = it.color,
                            frequency = it.frequency,
                            reminderTime = it.reminderTime,
                            targetCount = it.targetCount,
                            unit = it.unit,
                            isEditMode = true
                        )
                    }
                },
                onFailure = { /* Handle error */ }
            )
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun selectIcon(icon: HabitIcon) {
        _uiState.update { it.copy(selectedIcon = icon) }
    }

    fun selectColor(color: HabitColor) {
        _uiState.update { it.copy(selectedColor = color) }
    }

    fun updateFrequency(frequency: HabitFrequency) {
        _uiState.update { it.copy(frequency = frequency) }
    }

    fun updateTargetCount(count: Int) {
        if (count > 0) {
            _uiState.update { it.copy(targetCount = count) }
        }
    }

    fun updateUnit(unit: String) {
        _uiState.update { it.copy(unit = unit) }
    }

    fun updateReminderTime(time: LocalTime?) {
        _uiState.update { it.copy(reminderTime = time) }
    }

    fun updateArchived(isArchived: Boolean) {
        _uiState.update { it.copy(isArchived = isArchived) }
    }

    fun saveHabit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value

            if (state.isEditMode && habitId != null) {
                habitRepository.getHabitById(habitId).fold(
                    onSuccess = { existingHabit ->
                        existingHabit?.let { habit ->
                            habitRepository.updateHabit(
                                habit.copy(
                                    title = state.title,
                                    description = state.description,
                                    icon = state.selectedIcon,
                                    color = state.selectedColor,
                                    frequency = state.frequency,
                                    reminderTime = state.reminderTime?.toString(),
                                    isReminderEnabled = state.reminderTime != null,
                                    targetCount = state.targetCount,
                                    unit = state.unit
                                )
                            ).fold(
                                onSuccess = { onSuccess() },
                                onFailure = { /* handle error */ }
                            )
                        }
                    },
                    onFailure = { /* handle error */ }
                )
            } else {
                // Create new habit
                createHabitUseCase(
                    CreateHabitUseCase.Params(
                        title = state.title,
                        description = state.description,
                        icon = state.selectedIcon,
                        color = state.selectedColor,
                        frequency = state.frequency,
                        reminderTime = state.reminderTime,
                        targetCount = state.targetCount,
                        unit = state.unit
                    )
                ).fold(
                    onSuccess = { onSuccess() },
                    onFailure = { /* handle error */ }
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}