package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.example.habitstreak.core.util.UuidGenerator
import org.example.habitstreak.domain.model.Category
import org.example.habitstreak.domain.model.HabitColor
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.HabitIcon
import org.example.habitstreak.domain.repository.CategoryRepository
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.usecase.CreateHabitUseCase
import org.example.habitstreak.presentation.ui.state.CreateEditHabitUiState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CreateEditHabitViewModel(
    private val createHabitUseCase: CreateHabitUseCase,
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository,
    private val habitId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditHabitUiState())
    val uiState: StateFlow<CreateEditHabitUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        habitId?.let { loadHabit(it) }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().fold(
                onSuccess = { categories ->
                    _uiState.update { it.copy(availableCategories = categories) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    private fun loadHabit(id: String) {
        viewModelScope.launch {
            habitRepository.getHabitById(id).fold(
                onSuccess = { habit ->
                    habit?.let {
                        val reminderTime = it.reminderTime?.let { timeString ->
                            try {
                                LocalTime.parse(timeString)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        // Load categories for this habit
                        categoryRepository.getCategoriesForHabit(id).fold(
                            onSuccess = { categories ->
                                _uiState.value = CreateEditHabitUiState(
                                    title = it.title,
                                    description = it.description,
                                    selectedIcon = it.icon,
                                    selectedColor = it.color,
                                    frequency = it.frequency,
                                    selectedCategories = categories,
                                    availableCategories = _uiState.value.availableCategories,
                                    reminderTime = reminderTime,
                                    targetCount = it.targetCount,
                                    unit = it.unit,
                                    isEditMode = true
                                )
                            },
                            onFailure = { error ->
                                _uiState.update { it.copy(error = error.message) }
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
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

    fun toggleCategory(category: Category) {
        _uiState.update { state ->
            val currentCategories = state.selectedCategories
            val newCategories = if (currentCategories.contains(category)) {
                currentCategories - category
            } else {
                currentCategories + category
            }
            state.copy(selectedCategories = newCategories)
        }
    }

    fun showCustomCategoryDialog() {
        _uiState.update { it.copy(showCustomCategoryDialog = true) }
    }

    fun hideCustomCategoryDialog() {
        _uiState.update { it.copy(showCustomCategoryDialog = false, customCategoryName = "") }
    }

    fun updateCustomCategoryName(name: String) {
        _uiState.update { it.copy(customCategoryName = name) }
    }

    @OptIn(ExperimentalTime::class)
    fun createCustomCategory() {
        val categoryName = _uiState.value.customCategoryName.trim()
        if (categoryName.isBlank()) return

        viewModelScope.launch {
            // Check if category already exists
            categoryRepository.getCategoryByName(categoryName).fold(
                onSuccess = { existingCategory ->
                    if (existingCategory != null) {
                        // Category exists, just add it to selected
                        _uiState.update { state ->
                            state.copy(
                                selectedCategories = state.selectedCategories + existingCategory,
                                showCustomCategoryDialog = false,
                                customCategoryName = ""
                            )
                        }
                    } else {
                        // Create new category
                        val newCategory = Category(
                            id = UuidGenerator.generateUUID(),
                            name = categoryName,
                            isCustom = true,
                            usageCount = 0,
                            createdAt = Clock.System.now()
                        )

                        categoryRepository.createCategory(newCategory).fold(
                            onSuccess = { createdCategory ->
                                _uiState.update { state ->
                                    state.copy(
                                        selectedCategories = state.selectedCategories + createdCategory,
                                        availableCategories = state.availableCategories + createdCategory,
                                        showCustomCategoryDialog = false,
                                        customCategoryName = ""
                                    )
                                }
                            },
                            onFailure = { error ->
                                _uiState.update { it.copy(error = error.message) }
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
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

    @OptIn(ExperimentalTime::class)
    fun saveHabit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value

            if (state.isEditMode && habitId != null) {
                habitRepository.getHabitById(habitId).fold(
                    onSuccess = { existingHabit ->
                        existingHabit?.let { habit ->
                            // Update habit
                            habitRepository.updateHabit(
                                habit.copy(
                                    title = state.title,
                                    description = state.description,
                                    icon = state.selectedIcon,
                                    color = state.selectedColor,
                                    frequency = state.frequency,
                                    categories = state.selectedCategories,
                                    reminderTime = state.reminderTime?.toString(),
                                    isReminderEnabled = state.reminderTime != null,
                                    targetCount = state.targetCount,
                                    unit = state.unit
                                )
                            ).fold(
                                onSuccess = {
                                    // Update categories for habit
                                    categoryRepository.updateHabitCategories(
                                        habitId,
                                        state.selectedCategories.map { it.id }
                                    )

                                    // Update usage counts
                                    updateCategoryUsageCounts(
                                        oldCategories = habit.categories,
                                        newCategories = state.selectedCategories
                                    )

                                    onSuccess()
                                },
                                onFailure = { error ->
                                    _uiState.update { it.copy(error = error.message) }
                                }
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
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
                        categories = state.selectedCategories,
                        reminderTime = state.reminderTime,
                        targetCount = state.targetCount,
                        unit = state.unit
                    )
                ).fold(
                    onSuccess = { habit ->
                        onSuccess()
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
                )
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            selectedCategories = state.selectedCategories.filter { it.id != categoryId },
                            availableCategories = state.availableCategories.filter { it.id != categoryId }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    private suspend fun updateCategoryUsageCounts(
        oldCategories: List<Category>,
        newCategories: List<Category>
    ) {
        val removedCategories = oldCategories.filterNot { old ->
            newCategories.any { new -> new.id == old.id }
        }
        val addedCategories = newCategories.filterNot { new ->
            oldCategories.any { old -> old.id == new.id }
        }

        removedCategories.forEach { category ->
            categoryRepository.decrementUsageCount(category.id)
        }
        addedCategories.forEach { category ->
            categoryRepository.incrementUsageCount(category.id)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}