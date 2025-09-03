package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.example.habitstreak.domain.model.Category
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.repository.CategoryRepository
import org.example.habitstreak.domain.repository.HabitRecordRepository
import org.example.habitstreak.domain.usecase.ArchiveHabitUseCase
import org.example.habitstreak.domain.usecase.CalculateStreakUseCase
import org.example.habitstreak.domain.usecase.GetHabitsWithCompletionUseCase
import org.example.habitstreak.domain.usecase.ToggleHabitCompletionUseCase
import org.example.habitstreak.presentation.ui.state.HabitsUiState
import org.example.habitstreak.domain.util.DateProvider
import kotlin.collections.plus

class HabitsViewModel(
    private val getHabitsWithCompletionUseCase: GetHabitsWithCompletionUseCase,
    private val toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    private val calculateStreakUseCase: CalculateStreakUseCase,
    private val archiveHabitUseCase: ArchiveHabitUseCase,
    private val habitRecordRepository: HabitRecordRepository,
    private val categoryRepository: CategoryRepository,
    private val dateProvider: DateProvider
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(dateProvider.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _usedCategories = MutableStateFlow<List<Category>>(emptyList())
    val usedCategories: StateFlow<List<Category>> = _usedCategories.asStateFlow()

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val habitsWithCompletion = combine(
        _selectedDate,
        _selectedCategoryId
    ) { date, categoryId ->
        Pair(date, categoryId)
    }.flatMapLatest { (date, categoryId) ->
        getHabitsWithCompletionUseCase(date).combine(
            categoryRepository.observeUsedCategories()
        ) { habits, categories ->
            _usedCategories.value = categories

            // Filter by category if selected
            if (categoryId != null) {
                habits.filter { habitWithCompletion ->
                    habitWithCompletion.habit.categories.any { it.id == categoryId }
                }
            } else {
                habits
            }
        }
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

    private fun loadUsedCategories() {
        viewModelScope.launch {
            categoryRepository.observeUsedCategories().collect { categories ->
                _usedCategories.value = categories
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
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

    private fun observeAllRecordsForHistoryUpdate() {
        viewModelScope.launch {
            // Observe all records and update completion histories
            combine(
                habitRecordRepository.observeAllRecords(),
                habitsWithCompletion
            ) { allRecords, habits ->
                allRecords to habits
            }
                .debounce(300) // 300ms debounce to prevent frequent updates
                .collect { (allRecords, habits) ->
                    if (habits.isNotEmpty()) {
                        updateCompletionHistoriesFromRecords(allRecords, habits.map { it.habit })
                    }
                }
        }
    }

    private fun updateCompletionHistoriesFromRecords(records: List<HabitRecord>, habits: List<Habit>) {
        val today = dateProvider.today()
        val thirtyDaysAgo = today.minus(DatePeriod(days = 30))

        val historiesByHabit = mutableMapOf<String, Map<LocalDate, Boolean>>()

        habits.forEach { habit ->
            val habitRecords = records.filter { it.habitId == habit.id }
            val history = mutableMapOf<LocalDate, Boolean>()

            // Build completion history for last 30 days
            var currentDate = thirtyDaysAgo
            while (currentDate <= today) {
                val hasRecord = habitRecords.any { record ->
                    record.date == currentDate && record.completedCount >= habit.targetCount
                }
                history[currentDate] = hasRecord
                currentDate = currentDate.plus(DatePeriod(days = 1))
            }

            historiesByHabit[habit.id] = history
        }

        _uiState.update { state ->
            state.copy(completionHistories = historiesByHabit)
        }
    }

    private fun loadHabitDataSilently(habitId: String) {
        viewModelScope.launch {
            calculateStreakUseCase(habitId).fold(
                onSuccess = { streakInfo ->
                    _uiState.update { state ->
                        state.copy(
                            streaks = state.streaks + (habitId to streakInfo.currentStreak)
                        )
                    }
                },
                onFailure = { /* Silently continue */ }
            )
        }
    }

    fun updateHabitProgress(habitId: String, date: LocalDate, value: Int) {
        viewModelScope.launch {
            if (value == 0) {
                habitRecordRepository.markHabitAsIncomplete(habitId, date).fold(
                    onSuccess = {
                        // Auto-update through observeAllRecordsForHistoryUpdate
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
                )
            } else {
                habitRecordRepository.markHabitAsComplete(habitId, date, value).fold(
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
                        state.copy(error = error.message ?: "Bir hata oluştu")
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
                        state.copy(error = error.message ?: "Habit arşivlenemedi")
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

    fun isHabitLoading(habitId: String): Boolean {
        return _habitLoadingStates.value[habitId] == true
    }
}
}