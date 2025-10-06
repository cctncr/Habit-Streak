package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.example.habitstreak.domain.util.UuidGenerator
import org.example.habitstreak.domain.model.Category
import org.example.habitstreak.domain.model.HabitColor
import org.example.habitstreak.domain.model.HabitFrequency
import org.example.habitstreak.domain.model.HabitIcon
import org.example.habitstreak.domain.model.NotificationPeriod
import org.example.habitstreak.domain.repository.CategoryRepository
import org.example.habitstreak.domain.repository.HabitRepository
import org.example.habitstreak.domain.repository.PreferencesRepository
import org.example.habitstreak.domain.usecase.habit.CreateHabitUseCase
import org.example.habitstreak.domain.usecase.notification.HabitNotificationUseCase
import org.example.habitstreak.domain.usecase.notification.GlobalNotificationUseCase
import org.example.habitstreak.presentation.ui.state.CreateEditHabitUiState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.example.habitstreak.domain.usecase.notification.NotificationOperationResult
import org.example.habitstreak.domain.model.NotificationError
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.error_habit_created_reminder_failed
import habitstreak.composeapp.generated.resources.error_reminder_permission_required
import org.jetbrains.compose.resources.getString

class CreateEditHabitViewModel(
    private val createHabitUseCase: CreateHabitUseCase,
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val habitNotificationUseCase: HabitNotificationUseCase,
    private val globalNotificationUseCase: GlobalNotificationUseCase,
    private val habitId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditHabitUiState())
    val uiState: StateFlow<CreateEditHabitUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    init {
        loadCategories()
        observeGlobalNotificationStatus()
        habitId?.let { loadHabit(it) }
    }

    private fun observeGlobalNotificationStatus() {
        viewModelScope.launch {
            preferencesRepository.isNotificationsEnabled().collect { enabled ->
                _uiState.update { it.copy(isGlobalNotificationEnabled = enabled) }
            }
        }
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

    fun toggleNotification(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                // ✅ STEP 1: Check global notification status FIRST
                when (globalNotificationUseCase.checkStatus()) {
                    is GlobalNotificationUseCase.GlobalStatus.NeedsSystemPermission -> {
                        _uiEvents.tryEmit(UiEvent.RequestNotificationPermission)
                        _uiState.update { it.copy(isNotificationEnabled = false) }
                        return@launch
                    }

                    is GlobalNotificationUseCase.GlobalStatus.NeedsGlobalEnable -> {
                        _uiEvents.tryEmit(UiEvent.ShowGlobalEnableDialog(
                            _uiState.value.title.ifBlank { null }
                        ))
                        _uiState.update { it.copy(isNotificationEnabled = false) }
                        return@launch
                    }

                    else -> {
                        // Proceed with enabling
                    }
                }

                // ✅ STEP 2: Enable notification (only if permission/global OK)
                // Just update state - actual scheduling happens on saveHabit
                _uiState.update { it.copy(isNotificationEnabled = true) }

            } else {
                // Disable notification
                _uiState.update { it.copy(isNotificationEnabled = false) }
            }
        }
    }

    @Deprecated("Use toggleNotification() instead", ReplaceWith("toggleNotification(enabled)"))
    fun updateNotificationEnabled(enabled: Boolean) {
        toggleNotification(enabled)
    }

    fun updateNotificationPeriod(period: NotificationPeriod) {
        _uiState.update { it.copy(notificationPeriod = period) }
    }

    /**
     * Update both reminder time and notification period together
     * This ensures both are set atomically to avoid race conditions
     */
    fun updateReminderTimeAndPeriod(time: LocalTime, period: NotificationPeriod) {
        _uiState.update {
            it.copy(
                reminderTime = time,
                notificationPeriod = period,
                isNotificationEnabled = true
            )
        }
    }

    fun clearNotificationError() {
        _uiState.update { it.copy(notificationError = null) }
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

                                    // Update notification if reminder changed
                                    viewModelScope.launch {
                                        if (state.reminderTime != null && state.isNotificationEnabled) {
                                            // Update time and period atomically
                                            if (state.notificationPeriod != NotificationPeriod.EveryDay) {
                                                habitNotificationUseCase.updateTimeAndPeriod(
                                                    habitId = habitId,
                                                    time = state.reminderTime,
                                                    period = state.notificationPeriod
                                                )
                                            } else {
                                                // Just update time if period is default
                                                habitNotificationUseCase.enable(
                                                    habitId = habitId,
                                                    time = state.reminderTime
                                                )
                                            }
                                        } else if (state.reminderTime == null) {
                                            // Disable notification if reminder was removed
                                            habitNotificationUseCase.disable(habitId)
                                        }
                                    }

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
                        // If reminder is set, enable notification for the habit
                        if (state.reminderTime != null && state.isNotificationEnabled) {
                            viewModelScope.launch {
                                // Use atomic update if period is not default
                                val notificationResult = if (state.notificationPeriod != NotificationPeriod.EveryDay) {
                                    habitNotificationUseCase.updateTimeAndPeriod(
                                        habitId = habit.id,
                                        time = state.reminderTime,
                                        period = state.notificationPeriod
                                    )
                                } else {
                                    habitNotificationUseCase.enable(
                                        habitId = habit.id,
                                        time = state.reminderTime
                                    )
                                }

                                when (notificationResult) {
                                    is NotificationOperationResult.Success -> {
                                        // Success - all good
                                        onSuccess()
                                    }
                                    is NotificationOperationResult.Error -> {
                                        // Show warning but still navigate
                                        val errorMessage = getString(Res.string.error_habit_created_reminder_failed)
                                        _uiState.update {
                                            it.copy(
                                                error = errorMessage,
                                                notificationError = notificationResult.error
                                            )
                                        }
                                        onSuccess()
                                    }
                                    is NotificationOperationResult.PermissionRequired -> {
                                        val errorMessage = getString(Res.string.error_reminder_permission_required)
                                        _uiState.update {
                                            it.copy(
                                                error = errorMessage,
                                                notificationError = NotificationError.PermissionDenied(canRequestAgain = true)
                                            )
                                        }
                                        onSuccess()
                                    }
                                    else -> onSuccess()
                                }
                            }
                        } else {
                            // No notification - just navigate
                            onSuccess()
                        }
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

    fun resetForm() {
        if (!_uiState.value.isEditMode) {
            _uiState.value = CreateEditHabitUiState(
                availableCategories = _uiState.value.availableCategories
            )
        }
    }

    /**
     * UI Events for permission flow
     * Following the same pattern as HabitDetailViewModel
     */
    sealed class UiEvent {
        data object RequestNotificationPermission : UiEvent()
        data class ShowGlobalEnableDialog(val habitName: String?) : UiEvent()
        data object OpenAppSettings : UiEvent()
    }
}