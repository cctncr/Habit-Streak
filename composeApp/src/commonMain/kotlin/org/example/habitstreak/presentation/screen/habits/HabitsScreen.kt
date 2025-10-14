package org.example.habitstreak.presentation.screen.habits

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults.LinearStrokeCap
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import habitstreak.composeapp.generated.resources.Res
import habitstreak.composeapp.generated.resources.*
import org.example.habitstreak.domain.model.Category
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.presentation.ui.components.card.HabitCardFactory
import org.example.habitstreak.presentation.ui.components.common.DeleteCategoryDialog
import org.example.habitstreak.presentation.ui.components.common.ViewModeSelector
import org.example.habitstreak.presentation.ui.components.empty.EmptyHabitsState
import org.example.habitstreak.presentation.ui.model.ViewMode
import org.example.habitstreak.presentation.viewmodel.HabitsViewModel
import org.example.habitstreak.domain.model.HabitFilter
import org.example.habitstreak.domain.service.HabitFilterService
import org.example.habitstreak.domain.usecase.habit.GetHabitsWithCompletionUseCase
import org.example.habitstreak.domain.usecase.category.GetHabitsUsingCategoryUseCase
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import org.example.habitstreak.presentation.ui.components.category.getLocalizedCategoryName

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalTime::class)
@Composable
fun HabitsScreen(
    onNavigateToCreateHabit: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHabitDetail: (String) -> Unit,
    onFirstFrameRendered: () -> Unit = {},
    viewModel: HabitsViewModel = koinViewModel(),
    habitFilterService: HabitFilterService = koinInject()
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
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var affectedHabits by remember { mutableStateOf<List<Habit>>(emptyList()) }
    val currentViewMode = ViewMode.fromOrdinal(viewModeOrdinal)
    val coroutineScope = rememberCoroutineScope()
    val getHabitsUsingCategoryUseCase: GetHabitsUsingCategoryUseCase = koinInject()

    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var previousScrollIndex by remember { mutableStateOf(0) }
    var previousScrollOffset by remember { mutableStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(true) }

    val canReorder = selectedFilter == HabitFilter.ALL && selectedCategoryId == null

    val displayedHabits = remember {
        mutableStateOf<List<GetHabitsWithCompletionUseCase.HabitWithCompletion>>(emptyList())
    }
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragStartIndexInOriginal by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(habitsWithCompletion) {
        if (draggedItemId == null) {
            displayedHabits.value = habitsWithCompletion
        }
    }

    val filteredHabits =
        remember(displayedHabits.value, selectedFilter, selectedCategoryId, today) {
            derivedStateOf {
                habitFilterService.filterHabits(
                    habits = displayedHabits.value,
                    filter = selectedFilter,
                    selectedCategoryId = selectedCategoryId,
                    targetDate = today
                )
            }.value
        }

    val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
        if (canReorder) {
            val fromIndex = from.index - 2
            val toIndex = to.index - 2
            val habitCount = displayedHabits.value.size
            if (fromIndex >= 0 && toIndex >= 0 && fromIndex < habitCount && toIndex < habitCount) {
                if (draggedItemId == null) {
                    val itemId = displayedHabits.value[fromIndex].habit.id
                    draggedItemId = itemId
                    dragStartIndexInOriginal =
                        habitsWithCompletion.indexOfFirst { it.habit.id == itemId }
                }

                val newList = displayedHabits.value.toMutableList()
                val item = newList.removeAt(fromIndex)
                newList.add(toIndex, item)
                displayedHabits.value = newList
            }
        }
    }

    LaunchedEffect(reorderableLazyListState.isAnyItemDragging) {
        if (!reorderableLazyListState.isAnyItemDragging && draggedItemId != null) {
            val itemId = draggedItemId!!
            val originalFromIndex = dragStartIndexInOriginal ?: 0
            val newToIndex = displayedHabits.value.indexOfFirst { it.habit.id == itemId }

            if (newToIndex >= 0 && originalFromIndex != newToIndex) {
                viewModel.reorderHabits(originalFromIndex, newToIndex)
            }

            draggedItemId = null
            dragStartIndexInOriginal = null
        }
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

    // Progress statistics using service following SRP
    val progressStats by remember(habitsWithCompletion, today) {
        derivedStateOf {
            habitFilterService.calculateProgressStats(habitsWithCompletion, today)
        }
    }

    // Track first card rendering - this is the REAL indicator that UI is visible
    var hasRenderedFirstCard by remember { mutableStateOf(false) }

    // Handle empty state - if no habits, trigger callback immediately
    LaunchedEffect(habitsWithCompletion) {
        if (habitsWithCompletion.isEmpty() && !hasRenderedFirstCard) {
            hasRenderedFirstCard = true
            onFirstFrameRendered()
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
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(Res.string.content_desc_settings)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToStatistics) {
                        Icon(
                            Icons.Outlined.Analytics,
                            contentDescription = stringResource(Res.string.content_desc_statistics)
                        )
                    }
//                    IconButton(onClick = { showDatePicker = true }) {
//                        Icon(Icons.Outlined.CalendarMonth, contentDescription = stringResource(Res.string.content_desc_calendar))
//                    }
                    IconButton(onClick = onNavigateToCreateHabit) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = stringResource(Res.string.content_desc_add_habit)
                        )
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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 100.dp
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
                                completionPercentage = (progressStats.completionRate * 100).toInt(),
                                todayCompleted = progressStats.completedHabits,
                                totalHabits = progressStats.totalHabits,
                                onAssistChipClick = {
                                    selectedFilter = HabitFilter.PENDING
                                    viewModel.clearCategoryFilter()
                                }
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

                                if (usedCategories.isNotEmpty() && displayedHabits.value.isNotEmpty()) {
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
                                            val chipInteractionSource = remember { MutableInteractionSource() }
                                            Box {
                                                FilterChip(
                                                    selected = selectedCategoryId == category.id,
                                                    onClick = {},
                                                    label = {
                                                        Text(
                                                            getLocalizedCategoryName(category),
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    },
                                                    modifier = Modifier.height(28.dp),
                                                    interactionSource = chipInteractionSource
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .matchParentSize()
                                                        .combinedClickable(
                                                            onClick = { viewModel.selectCategory(category.id) },
                                                            onLongClick = {
                                                                categoryToDelete = category
                                                                coroutineScope.launch {
                                                                    getHabitsUsingCategoryUseCase(category.id).fold(
                                                                        onSuccess = { habits ->
                                                                            affectedHabits = habits
                                                                        },
                                                                        onFailure = {
                                                                            affectedHabits = emptyList()
                                                                        }
                                                                    )
                                                                }
                                                            },
                                                            interactionSource = chipInteractionSource,
                                                            indication = null
                                                        )
                                                )
                                            }
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
                            ReorderableItem(
                                reorderableLazyListState,
                                key = habitWithCompletion.habit.id
                            ) {
                                val habitId = habitWithCompletion.habit.id
                                val completionHistory =
                                    uiState.completionHistories[habitId] ?: emptyMap()
                                val todayProgress = habitWithCompletion.completedCount.toFloat() /
                                        habitWithCompletion.habit.targetCount.coerceAtLeast(1)

                                // Check if this is the first card
                                val isFirstCard = habitWithCompletion == filteredHabits.firstOrNull()

                                val cardModifier = Modifier.animateItem()
                                val finalModifier = if (canReorder) {
                                    cardModifier.longPressDraggableHandle()
                                } else {
                                    cardModifier
                                }

                                // Add onGloballyPositioned to the FIRST card only
                                val modifierWithCallback = if (isFirstCard && !hasRenderedFirstCard) {
                                    finalModifier.onGloballyPositioned {
                                        // First card is positioned = UI is actually rendered!
                                        hasRenderedFirstCard = true
                                        onFirstFrameRendered()
                                    }
                                } else {
                                    finalModifier
                                }

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
                                    modifier = modifierWithCallback
                                )
                            }
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
                    Text(
                        stringResource(Res.string.action_archive),
                        color = MaterialTheme.colorScheme.error
                    )
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

    categoryToDelete?.let { category ->
        DeleteCategoryDialog(
            category = category,
            affectedHabits = affectedHabits,
            onConfirm = {
                viewModel.deleteCategory(category.id)
                categoryToDelete = null
                affectedHabits = emptyList()
            },
            onDismiss = {
                categoryToDelete = null
                affectedHabits = emptyList()
            }
        )
    }
}

@Composable
private fun ProgressOverviewCard(
    completionPercentage: Int,
    todayCompleted: Int,
    totalHabits: Int,
    onAssistChipClick: () -> Unit = {}
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
                        onClick = onAssistChipClick,
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
                strokeCap = LinearStrokeCap
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


@Composable
fun HabitFilter.getLabel(): String = when (this) {
    HabitFilter.ALL -> stringResource(Res.string.filter_all_habits)
    HabitFilter.COMPLETED -> stringResource(Res.string.filter_done)
    HabitFilter.PENDING -> stringResource(Res.string.filter_todo)
}