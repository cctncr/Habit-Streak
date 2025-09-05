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
import org.example.habitstreak.domain.model.Category
import org.example.habitstreak.presentation.ui.components.card.HabitCard
import org.example.habitstreak.presentation.ui.components.card.HabitCardFactory
import org.example.habitstreak.presentation.ui.components.common.ViewModeSelector
import org.example.habitstreak.presentation.ui.components.empty.EmptyHabitsState
import org.example.habitstreak.presentation.ui.model.ViewMode
import org.example.habitstreak.presentation.viewmodel.HabitsViewModel
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

    var selectedFilter by remember { mutableStateOf(HabitFilter.ALL) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }
    var currentViewMode by remember { mutableStateOf<ViewMode>(ViewMode.Medium) }

    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Detect scroll direction
    val isScrollingUp by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 ||
                    listState.firstVisibleItemScrollOffset < 100
        }
    }

    val filteredHabits = remember(habitsWithCompletion, selectedFilter, selectedCategoryId) {
        var filtered = when (selectedFilter) {
            HabitFilter.ALL -> habitsWithCompletion
            HabitFilter.COMPLETED -> habitsWithCompletion.filter {
                it.completedCount >= it.habit.targetCount
            }
            HabitFilter.PENDING -> habitsWithCompletion.filter {
                it.completedCount < it.habit.targetCount
            }
        }

        selectedCategoryId?.let { categoryId ->
            filtered = filtered.filter { habitWithCompletion ->
                habitWithCompletion.habit.categories.any { it.id == categoryId }
            }
        }

        filtered
    }

    val todayCompleted = filteredHabits.count { it.completedCount >= it.habit.targetCount }
    val totalHabits = filteredHabits.size
    val completionPercentage = if (totalHabits > 0) {
        (todayCompleted * 100) / totalHabits
    } else 0

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Habits",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToStatistics) {
                        Icon(Icons.Outlined.Analytics, contentDescription = "Statistics")
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar")
                    }
                    IconButton(onClick = onNavigateToCreateHabit) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add Habit")
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
                                completionPercentage = completionPercentage,
                                todayCompleted = todayCompleted,
                                totalHabits = totalHabits
                            )
                        }

                        item(key = "filters") {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Filter chips
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(HabitFilter.entries) { filter ->
                                        FilterChip(
                                            selected = selectedFilter == filter,
                                            onClick = { selectedFilter = filter },
                                            label = { Text(filter.label) }
                                        )
                                    }
                                }

                                // Category filter if categories exist
                                if (usedCategories.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item {
                                            FilterChip(
                                                selected = selectedCategoryId == null,
                                                onClick = { viewModel.clearCategoryFilter() },
                                                label = { Text("All Categories") }
                                            )
                                        }
                                        items(usedCategories) { category ->
                                            FilterChip(
                                                selected = selectedCategoryId == category.id,
                                                onClick = { viewModel.selectCategory(category.id) },
                                                label = { Text(category.name) }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
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
                isVisible = !isScrollingUp && filteredHabits.isNotEmpty(),
                onModeSelected = { mode ->
                    currentViewMode = mode
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
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
            title = { Text("Archive Habit?") },
            text = { Text("This will hide the habit but keep all your data.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.archiveHabit(habitId)
                        showDeleteConfirmation = null
                    }
                ) {
                    Text("Archive", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
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
                        text = "Daily Goal",
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EnhancedFilterChipsRow(
    selectedFilter: HabitFilter,
    onFilterSelected: (HabitFilter) -> Unit,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    usedCategories: List<Category>,
    habitsCount: Map<HabitFilter, Int>
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mevcut filter chips
        items(HabitFilter.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter && selectedCategoryId == null,
                onClick = {
                    onFilterSelected(filter)
                    onCategorySelected(null)
                },
                label = { Text("${filter.label} (${habitsCount[filter] ?: 0})") },
                leadingIcon = if (selectedFilter == filter && selectedCategoryId == null) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null
            )
        }

        // Divider
        if (usedCategories.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.width(8.dp))
                VerticalDivider(
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 4.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        // Category chips
        items(
            items = usedCategories,
            key = { it.id }
        ) { category ->
            FilterChip(
                onClick = {
                    if (selectedCategoryId == category.id) {
                        onCategorySelected(null)
                    } else {
                        onCategorySelected(category.id)
                    }
                },
                selected = selectedCategoryId == category.id,
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category.name)
                        if (category.usageCount > 0) {
                            Badge(
                                containerColor = if (selectedCategoryId == category.id) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                }
                            ) {
                                Text(
                                    text = category.usageCount.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                },
                leadingIcon = if (selectedCategoryId == category.id) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null
            )
        }
    }
}


@Composable
private fun SectionHeader(title: String, count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "$count habits",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

enum class HabitFilter(val label: String) {
    ALL("All"),
    COMPLETED("Done"),
    PENDING("Todo")
}