package org.example.habitstreak.presentation.screen.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.example.habitstreak.presentation.ui.components.card.HabitCard
import org.example.habitstreak.presentation.ui.components.empty.EmptyHabitsState
import org.example.habitstreak.presentation.viewmodel.HabitsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HabitsScreen(
    onNavigateToCreateHabit: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HabitsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val habitsWithCompletion by viewModel.habitsWithCompletion.collectAsState()
    val today by viewModel.selectedDate.collectAsState()

    var selectedFilter by remember { mutableStateOf(HabitFilter.ALL) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    // Calculate stats
    val todayCompleted = habitsWithCompletion.count { it.isCompletedToday }
    val totalHabits = habitsWithCompletion.size
    val completionPercentage = if (totalHabits > 0) {
        (todayCompleted.toFloat() / totalHabits * 100).toInt()
    } else 0

    // Filter habits
    val filteredHabits = remember(habitsWithCompletion, selectedFilter) {
        when (selectedFilter) {
            HabitFilter.ALL -> habitsWithCompletion
            HabitFilter.COMPLETED -> habitsWithCompletion.filter { it.isCompletedToday }
            HabitFilter.PENDING -> habitsWithCompletion.filter { !it.isCompletedToday }
            HabitFilter.HIGH_STREAK -> habitsWithCompletion.filter {
                (uiState.streaks[it.habit.id] ?: 0) >= 7
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Today's Progress",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        AnimatedVisibility(visible = totalHabits > 0) {
                            Text(
                                text = "$todayCompleted of $totalHabits habits completed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreateHabit,
                expanded = listState.firstVisibleItemIndex == 0,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("New Habit") }
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "progress") {
                            ProgressOverviewCard(
                                completionPercentage = completionPercentage,
                                todayCompleted = todayCompleted,
                                totalHabits = totalHabits,
                                streakDays = uiState.streaks.values.maxOrNull() ?: 0
                            )
                        }

                        item(key = "stats") {
                            QuickStatsRow(
                                totalCompleted = uiState.completionHistories.values
                                    .flatMap { it.entries }
                                    .count { it.value >= 1f },
                                activeStreaks = uiState.streaks.count { it.value > 0 },
                                bestStreak = uiState.streaks.values.maxOrNull() ?: 0
                            )
                        }

                        item(key = "filters") {
                            FilterChipsRow(
                                selectedFilter = selectedFilter,
                                onFilterSelected = { selectedFilter = it },
                                habitsCount = mapOf(
                                    HabitFilter.ALL to habitsWithCompletion.size,
                                    HabitFilter.COMPLETED to todayCompleted,
                                    HabitFilter.PENDING to (totalHabits - todayCompleted),
                                    HabitFilter.HIGH_STREAK to habitsWithCompletion.count {
                                        (uiState.streaks[it.habit.id] ?: 0) >= 7
                                    }
                                )
                            )
                        }

                        stickyHeader(key = "header") {
                            SectionHeader(
                                title = when(selectedFilter) {
                                    HabitFilter.ALL -> "All Habits"
                                    HabitFilter.COMPLETED -> "Completed Today"
                                    HabitFilter.PENDING -> "Pending Habits"
                                    HabitFilter.HIGH_STREAK -> "On Fire 🔥"
                                },
                                count = filteredHabits.size
                            )
                        }

                        items(
                            items = filteredHabits,
                            key = { it.habit.id }
                        ) { habitWithCompletion ->
                            val habitId = habitWithCompletion.habit.id
                            val completionHistory = uiState.completionHistories[habitId] ?: emptyMap()
                            val todayProgress = habitWithCompletion.completedCount.toFloat() /
                                    habitWithCompletion.habit.targetCount.coerceAtLeast(1)

                            SwipeableHabitCard(
                                habit = habitWithCompletion.habit,
                                completionHistory = completionHistory,
                                todayProgress = todayProgress,
                                currentStreak = uiState.streaks[habitId] ?: 0,
                                today = today,
                                onUpdateProgress = { date, value ->
                                    viewModel.updateHabitProgress(habitId, date, value)
                                },
                                onDelete = {
                                    showDeleteConfirmation = habitId
                                },
                                modifier = Modifier.animateItem()
                            )
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

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
    totalHabits: Int,
    streakDays: Int
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

                if (streakDays > 0) {
                    AssistChip(
                        onClick = { },
                        label = { Text("$streakDays day streak") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFF6B35)
                            )
                        }
                    )
                }
            }

            LinearProgressIndicator(
                progress = { completionPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun QuickStatsRow(
    totalCompleted: Int,
    activeStreaks: Int,
    bestStreak: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.CheckCircle,
            value = totalCompleted.toString(),
            label = "Completed",
            color = MaterialTheme.colorScheme.primary
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.LocalFireDepartment,
            value = activeStreaks.toString(),
            label = "Active",
            color = Color(0xFFFF6B35)
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.EmojiEvents,
            value = bestStreak.toString(),
            label = "Best",
            color = Color(0xFFFFD700)
        )
    }
}

@Composable
private fun QuickStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    selectedFilter: HabitFilter,
    onFilterSelected: (HabitFilter) -> Unit,
    habitsCount: Map<HabitFilter, Int>
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HabitFilter.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text("${filter.label} (${habitsCount[filter] ?: 0})") },
                leadingIcon = if (selectedFilter == filter) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
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
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("$count habits", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableHabitCard(
    habit: org.example.habitstreak.domain.model.Habit,
    completionHistory: Map<LocalDate, Float>,
    todayProgress: Float,
    currentStreak: Int,
    today: LocalDate,
    onUpdateProgress: (LocalDate, Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSwipeAction by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        // Background delete action
        AnimatedVisibility(
            visible = showSwipeAction,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Main card with swipe gesture
        HabitCard(
            habit = habit,
            completionHistory = completionHistory,
            todayProgress = todayProgress,
            currentStreak = currentStreak,
            today = today,
            onUpdateProgress = onUpdateProgress,
            onCardClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        if (delta < -50) showSwipeAction = true
                        if (delta > 50) showSwipeAction = false
                    }
                )
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
    PENDING("Todo"),
    HIGH_STREAK("Streaking")
}