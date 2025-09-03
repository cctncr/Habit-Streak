package org.example.habitstreak.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    private val _habitLoadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

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

    // Yeni eklenen fonksiyon - Tüm habit records'larını observe ediyor
    private fun observeAllRecordsForHistoryUpdate() {
        viewModelScope.launch {
            // Tüm records'ları observe et ve değişiklikleri completion history'ye yansıt
            combine(
                habitRecordRepository.observeAllRecords(),
                habitsWithCompletion
            ) { allRecords, habits ->
                allRecords to habits
            }
                .debounce(300) // 300ms debounce ile sık güncellemeyi engelle
                .collect { (allRecords, habits) ->
                    if (habits.isNotEmpty()) {
                        updateCompletionHistoriesFromRecords(allRecords, habits.map { it.habit })
                    }
                }
        }
    }

    // Yeni fonksiyon - Records'lardan completion histories'i güncelliyor
    private suspend fun updateCompletionHistoriesFromRecords(
        allRecords: List<HabitRecord>,
        habits: List<Habit>
    ) {
        val today = dateProvider.today()
        val startDate = today.minus(DatePeriod(days = 364))

        // Sadece son 1 yıl içindeki records'ları filtrele
        val recentRecords = allRecords.filter {
            it.date >= startDate && it.date <= today
        }

        val newHistories = mutableMapOf<String, Map<LocalDate, Float>>()
        val newStreaks = mutableMapOf<String, Int>()

        // Her habit için history hesapla
        habits.forEach { habit ->
            val habitRecords = recentRecords.filter { it.habitId == habit.id }
            val targetCount = habit.targetCount.coerceAtLeast(1)

            // Completion history oluştur
            val completionMap = habitRecords.associate { record ->
                record.date to (record.completedCount.toFloat() / targetCount)
            }
            newHistories[habit.id] = completionMap

            // Streak hesapla (async)
            viewModelScope.launch {
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

        // History'leri güncelle
        _uiState.update { state ->
            state.copy(completionHistories = newHistories.toMap())
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
}