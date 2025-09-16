package org.example.habitstreak.presentation.screen.habits

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*
import org.example.habitstreak.domain.model.Category
import org.example.habitstreak.presentation.ui.components.card.HabitCard
import org.example.habitstreak.presentation.ui.components.card.HabitCardFactory
import org.example.habitstreak.presentation.ui.components.common.ViewModeSelector
import org.example.habitstreak.presentation.ui.components.empty.EmptyHabitsState
import org.example.habitstreak.presentation.ui.model.ViewMode
import org.example.habitstreak.presentation.viewmodel.HabitsViewModel
import org.example.habitstreak.domain.usecase.habit.GetHabitsWithCompletionUseCase
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HabitsScreen(
    onNavigateToCreateHabit: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHabitDetail: (String) -> Unit,
    viewModel: HabitsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val habitsWithCompletion by viewModel.habitsWithCompletion.collectAsState()
    val today by viewModel.selectedDate.collectAsState()
    val usedCategories by viewModel.usedCategories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val viewModeOrdinal by viewModel.currentViewMode.collectAsState()

    var selectedFilter by remember { mutableStateOf(HabitFilter.ALL) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }
    val currentViewMode = ViewMode.fromOrdinal(viewModeOrdinal)

    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var previousScrollIndex by remember { mutableStateOf(0) }
    var previousScrollOffset by remember { mutableStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(true) }

    val filteredHabits = remember(habitsWithCompletion, selectedFilter, selectedCategoryId) {
        derivedStateOf {
            val filterPredicate: (GetHabitsWithCompletionUseCase.HabitWithCompletion) -> Boolean = when (selectedFilter) {
                HabitFilter.ALL -> { _ -> true }
                HabitFilter.COMPLETED -> { habitWithCompletion -> habitWithCompletion.completedCount >= habitWithCompletion.habit.targetCount }
                HabitFilter.PENDING -> { habitWithCompletion -> habitWithCompletion.completedCount < habitWithCompletion.habit.targetCount }
            }

            val categoryPredicate: (GetHabitsWithCompletionUseCase.HabitWithCompletion) -> Boolean =
                selectedCategoryId?.let { categoryId ->
                    { habitWithCompletion -> habitWithCompletion.habit.categories.any { category -> category.id == categoryId } }
                } ?: { _ -> true }

            habitsWithCompletion.filter { habitWithCompletion ->
                filterPredicate(habitWithCompletion) && categoryPredicate(habitWithCompletion)
            }
        }.value
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentIndex = listState.firstVisibleItemIndex
        val currentOffset = listState.firstVisibleItemScrollOffset

        isScrollingUp = when {
            currentIndex < previousScrollIndex -> true
            currentIndex > previousScrollIndex -> false
            currentOffset < previousScrollOffset -> true
            currentOffset > previousScrollOffset -> false
            else -> isScrollingUp
        }

        previousScrollIndex = currentIndex
        previousScrollOffset = currentOffset
    }

    val todayCompleted = filteredHabits.count { it.completedCount >= it.habit.targetCount }
    val totalHabits = filteredHabits.size

    // Daily Goal hesaplaması - Tüm habitler üzerinden, filter'dan bağımsız
    val dailyGoalPercentage by remember {
        derivedStateOf {
            if (habitsWithCompletion.isEmpty()) {
                0
            } else {
                val totalProgress = habitsWithCompletion.sumOf { habitWithCompletion ->
                    val progress = habitWithCompletion.completedCount.toFloat() /
                            habitWithCompletion.habit.targetCount.coerceAtLeast(1)
                    (progress.coerceIn(0f, 1f) * 100).toInt()
                }
                (totalProgress / habitsWithCompletion.size).coerceIn(0, 100)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.screen_habits),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(Res.string.content_desc_settings))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToStatistics) {
                        Icon(Icons.Outlined.Analytics, contentDescription = stringResource(Res.string.content_desc_statistics))
                    }
//                    IconButton(onClick = { showDatePicker = true }) {
//                        Icon(Icons.Outlined.CalendarMonth, contentDescription = stringResource(Res.string.content_desc_calendar))
//                    }
                    IconButton(onClick = onNavigateToCreateHabit) {
                        Icon(Icons.Outlined.Add, contentDescription = stringResource(Res.string.content_desc_add_habit))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LoadingState()
                habitsWithCompletion.isEmpty() -> EmptyHabitsState(onCreateHabit = onNavigateToCreateHabit)
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 100.dp // Space for bottom selector
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            when (currentViewMode) {
                                is ViewMode.Large -> 16.dp
                                is ViewMode.Medium -> 12.dp
                                is ViewMode.Compact -> 8.dp
                            }
                        )
                    ) {
                        item(key = "progress") {
                            ProgressOverviewCard(
                                completionPercentage = dailyGoalPercentage,
                                todayCompleted = habitsWithCompletion.count {
                                    it.completedCount >= it.habit.targetCount
                                },
                                totalHabits = habitsWithCompletion.size
                            )
                        }

                        item(key = "filters") {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Spacer(modifier = Modifier.height(4.dp))

                                // Compact Filter chips
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(HabitFilter.entries) { filter ->
                                        FilterChip(
                                            selected = selectedFilter == filter,
                                            onClick = { selectedFilter = filter },
                                            label = {
                                                Text(
                                                    filter.getLabel(),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            },
                                            modifier = Modifier.height(32.dp)
                                        )
                                    }
                                }

                                // Compact Category filter
                                if (usedCategories.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        item {
                                            FilterChip(
                                                selected = selectedCategoryId == null,
                                                onClick = { viewModel.clearCategoryFilter() },
                                                label = {
                                                    Text(
                                                        stringResource(Res.string.filter_all_habits),
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                },
                                                modifier = Modifier.height(28.dp)
                                            )
                                        }
                                        items(usedCategories) { category ->
                                            FilterChip(
                                                selected = selectedCategoryId == category.id,
                                                onClick = { viewModel.selectCategory(category.id) },
                                                label = {
                                                    Text(
                                                        category.name,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                },
                                                modifier = Modifier.height(28.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }

                        items(
                            items = filteredHabits,
                            key = { it.habit.id }
                        ) { habitWithCompletion ->
                            val habitId = habitWithCompletion.habit.id
                            val completionHistory = uiState.completionHistories[habitId] ?: emptyMap()
                            val todayProgress = habitWithCompletion.completedCount.toFloat() /
                                    habitWithCompletion.habit.targetCount.coerceAtLeast(1)

                            HabitCardFactory.CreateCard(
                                viewMode = currentViewMode,
                                habit = habitWithCompletion.habit,
                                completionHistory = completionHistory,
                                todayProgress = todayProgress,
                                currentStreak = uiState.streaks[habitId] ?: 0,
                                today = today,
                                todayRecord = habitWithCompletion.todayRecord,
                                habitRecords = uiState.allRecords.filter { it.habitId == habitId },
                                onUpdateProgress = { date, value, note ->
                                    viewModel.updateHabitProgress(habitId, date, value, note)
                                },
                                onCardClick = {
                                    onNavigateToHabitDetail(habitId)
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }

            // View Mode Selector at bottom
            ViewModeSelector(
                currentMode = currentViewMode,
                isVisible = isScrollingUp,
                onModeSelected = { mode ->
                    viewModel.setViewMode(
                        when (mode) {
                            is ViewMode.Large -> 0
                            is ViewMode.Medium -> 1
                            is ViewMode.Compact -> 2
                        }
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(Res.string.dismiss))
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteConfirmation?.let { habitId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text(stringResource(Res.string.archive_habit_title)) },
            text = { Text(stringResource(Res.string.archive_habit_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.archiveHabit(habitId)
                        showDeleteConfirmation = null
                    }
                ) {
                    Text(stringResource(Res.string.action_archive), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }

    // Date Picker
    if (showDatePicker) {
        DatePickerModal(
            selectedDate = today,
            onDateSelected = { date ->
                viewModel.selectDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun ProgressOverviewCard(
    completionPercentage: Int,
    todayCompleted: Int,
    totalHabits: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$completionPercentage%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.daily_goal),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                if (totalHabits > 0) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text("$todayCompleted / $totalHabits")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            LinearProgressIndicator(
                progress = { completionPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                strokeCap = androidx.compose.material3.ProgressIndicatorDefaults.LinearStrokeCap
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDays() * 24 * 60 * 60 * 1000
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val epochDays = millis / (24 * 60 * 60 * 1000)
                        val date = LocalDate.fromEpochDays(epochDays.toInt())
                        onDateSelected(date)
                    }
                }
            ) {
                Text(stringResource(Res.string.select))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

enum class HabitFilter {
    ALL,
    COMPLETED,
    PENDING
}

@Composable
fun HabitFilter.getLabel(): String = when (this) {
    HabitFilter.ALL -> stringResource(Res.string.filter_all_habits)
    HabitFilter.COMPLETED -> stringResource(Res.string.filter_done)
    HabitFilter.PENDING -> stringResource(Res.string.filter_todo)
}