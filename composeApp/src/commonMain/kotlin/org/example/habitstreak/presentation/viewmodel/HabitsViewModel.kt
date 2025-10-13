package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.example.habitstreak.domain.model.Category
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.repository.CategoryRepository
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.usecase.habit.ArchiveHabitUseCase
import org.example.habitstreak.domain.usecase.habit.CalculateStreakUseCase
import org.example.habitstreak.domain.usecase.habit.GetHabitsWithCompletionUseCase
import org.example.habitstreak.domain.usecase.habit.ToggleHabitCompletionUseCase
import org.example.habitstreak.presentation.ui.state.HabitsUiState
import org.example.habitstreak.domain.util.DateProvider
import kotlin.collections.plus

class HabitsViewModel(
    private val getHabitsWithCompletionUseCase: GetHabitsWithCompletionUseCase,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val archiveHabitUseCase: ArchiveHabitUseCase,
    private val reorderHabitsUseCase: org.example.habitstreak.domain.usecase.habit.ReorderHabitsUseCase,
    private val habitRecordRepository: HabitRecordRepository,
    private val categoryRepository: CategoryRepository,
    private val dateProvider: DateProvider
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(dateProvider.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    private val _habitLoadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _currentViewMode = MutableStateFlow<Int>(1) // Default: Medium
    val currentViewMode: StateFlow<Int> = _currentViewMode.asStateFlow()

    private val _usedCategories = MutableStateFlow<List<Category>>(emptyList())
    val usedCategories: StateFlow<List<Category>> = _usedCategories.asStateFlow()

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
        observeAllRecordsForHistoryUpdate()
        loadUsedCategories()
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

                // Load streaks and histories for all habits
                habits.forEach { habitWithCompletion ->
                    loadHabitDataSilently(habitWithCompletion.habit.id)
                }
            }
        }
    }

    private fun loadUsedCategories() {
        viewModelScope.launch {
            categoryRepository.observeUsedCategories()
                .catch { e ->
                    _uiState.update { it.copy(error = "Failed to load categories") }
                    _usedCategories.value = emptyList()
                }
                .collect { categories ->
                    _usedCategories.value = categories
                }
        }
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun clearCategoryFilter() {
        _selectedCategoryId.value = null
    }

    fun setViewMode(modeOrdinal: Int) {
        _currentViewMode.value = modeOrdinal
    }

    private fun observeAllRecordsForHistoryUpdate() {
        viewModelScope.launch {
            combine(
                habitRecordRepository.observeAllRecords(),
                habitsWithCompletion
            ) { allRecords, habits ->
                allRecords to habits
            }
                .debounce(500) // Debounce artırıldı
                .collect { (allRecords, habits) ->
                    if (habits.isNotEmpty() && allRecords.isNotEmpty()) {
                        updateCompletionHistoriesFromRecords(allRecords, habits.map { it.habit })
                    }
                }
        }
    }

    private suspend fun updateCompletionHistoriesFromRecords(
        allRecords: List<HabitRecord>,
        habits: List<Habit>
    ) {
        val today = dateProvider.today()
        val minHistoryDays = 180
        val oldestRecordDate = allRecords.minOfOrNull { it.date }
        val minStartDate = today.minus(DatePeriod(days = minHistoryDays))

        val effectiveStartDate = if (oldestRecordDate != null && oldestRecordDate < minStartDate) {
            oldestRecordDate
        } else {
            minStartDate
        }

        val recentRecords = allRecords.filter {
            it.date in effectiveStartDate..today
        }

        if (recentRecords.isEmpty()) return

        val newHistories = mutableMapOf<String, Map<LocalDate, Float>>()
        val habitRecordsMap = recentRecords.groupBy { it.habitId } // Tek seferde grupla

        habits.forEach { habit ->
            val habitRecords = habitRecordsMap[habit.id] ?: emptyList()
            if (habitRecords.isNotEmpty()) {
                val targetCount = habit.targetCount.coerceAtLeast(1)
                val completionMap = habitRecords.associate { record ->
                    record.date to (record.completedCount.toFloat() / targetCount)
                }
                newHistories[habit.id] = completionMap
            }
        }

        _uiState.update { state ->
            state.copy(
                completionHistories = newHistories,
                allRecords = allRecords
            )
        }

        habits.chunked(5).forEach { habitChunk ->
            viewModelScope.launch {
                habitChunk.forEach { habit ->
                    calculateStreakUseCase(habit.id).fold(
                        onSuccess = { streakInfo ->
                            _uiState.update { state ->
                                state.copy(
                                    streaks = state.streaks + (habit.id to streakInfo.currentStreak)
                                )
                            }
                        },
                        onFailure = { /* Ignore */ }
                    )
                }
            }
        }
    }

    private fun loadHabitDataSilently(habitId: String) {
        // Bu fonksiyonu basitleştir, observeAllRecordsForHistoryUpdate zaten güncelleme yapıyor
        viewModelScope.launch {
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
        }
    }

    fun updateHabitProgress(habitId: String, date: LocalDate, value: Int, note: String = "") {
        viewModelScope.launch {
            if (value == 0 && note.isBlank()) {
                habitRecordRepository.markHabitAsIncomplete(habitId, date).fold(
                    onSuccess = {
                        // Auto-update through observeAllRecordsForHistoryUpdate
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
                )
            } else {
                habitRecordRepository.markHabitAsComplete(habitId, date, value, note).fold(
                    onSuccess = {
                        // Auto-update through observeAllRecordsForHistoryUpdate
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
                )
            }
        }
    }

    fun toggleHabitCompletion(habitId: String) {
        viewModelScope.launch {
            _habitLoadingStates.update { it + (habitId to true) }

            toggleHabitCompletionUseCase(
                ToggleHabitCompletionUseCase.Params(
                    habitId = habitId,
                    date = _selectedDate.value
                )
            ).fold(
                onSuccess = {
                    // Auto-update through observeAllRecordsForHistoryUpdate
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

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId).fold(
                onSuccess = {
                    if (_selectedCategoryId.value == categoryId) {
                        _selectedCategoryId.value = null
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
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

    fun isHabitLoading(habitId: String): Boolean {
        return _habitLoadingStates.value[habitId] == true
    }

    fun reorderHabits(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentHabits = habitsWithCompletion.value.map { it.habit }
            if (fromIndex < 0 || toIndex < 0 || fromIndex >= currentHabits.size || toIndex >= currentHabits.size) {
                return@launch
            }

            reorderHabitsUseCase(
                org.example.habitstreak.domain.usecase.habit.ReorderHabitsUseCase.Params(
                    habits = currentHabits,
                    fromIndex = fromIndex,
                    toIndex = toIndex
                )
            ).fold(
                onSuccess = { },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(error = error.message ?: "Failed to reorder habits")
                    }
                }
            )
        }
    }
}