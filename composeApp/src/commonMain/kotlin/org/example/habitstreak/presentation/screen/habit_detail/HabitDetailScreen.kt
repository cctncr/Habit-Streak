package org.example.habitstreak.presentation.screen.habit_detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.example.habitstreak.domain.model.Habit
import org.example.habitstreak.domain.model.HabitRecord
import org.example.habitstreak.domain.util.DateProvider
import org.example.habitstreak.presentation.ui.components.CalendarView
import org.example.habitstreak.presentation.ui.components.NotificationSettingsCard
import org.example.habitstreak.presentation.ui.components.ProgressCard
import org.example.habitstreak.presentation.ui.components.StatsCard
import org.example.habitstreak.presentation.ui.theme.AppTheme
import org.example.habitstreak.presentation.ui.theme.HabitStreakTheme
import org.example.habitstreak.presentation.ui.utils.DateFormatter
import org.example.habitstreak.presentation.viewmodel.HabitDetailViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.ExperimentalTime

// Enum definitions
enum class StatsTimeFilter(val label: String) {
    ALL_TIME("All Time"),
    THIS_YEAR("This Year"),
    THIS_MONTH("This Month"),
    THIS_WEEK("This Week"),
    LAST_30_DAYS("Last 30 Days")
}

enum class ActivityTab(val label: String) {
    HISTORY("History"),
    NOTES("Notes")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun HabitDetailScreen(
    habitId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    viewModel: HabitDetailViewModel = koinViewModel(key = habitId) { parametersOf(habitId) },
    dateProvider: DateProvider = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedActivityTab by remember { mutableStateOf(ActivityTab.HISTORY) }
    var selectedStatsFilter by remember { mutableStateOf(StatsTimeFilter.ALL_TIME) }

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HabitDetailViewModel.UiEvent.RequestNotificationPermission -> {
                    // Platform-specific handler will handle this
                    handlePermissionRequest(viewModel)
                }
                is HabitDetailViewModel.UiEvent.OpenAppSettings -> {
                    // Platform-specific handler will handle this
                    handleOpenSettings()
                }
            }
        }
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.habit?.title ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete Habit") },
                                    onClick = {
                                        showMoreMenu = false
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                uiState.habit?.let { habit ->
                    // Progress Section
                    ProgressCard(
                        stats = uiState.stats,
                        habit = habit,
                        modifier = Modifier.padding(16.dp)
                    )

                    // Stats Section
                    StatsCard(
                        stats = uiState.stats,
                        filter = selectedStatsFilter,
                        onFilterChange = { selectedStatsFilter = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // Calendar Section
                    CalendarView(
                        currentMonth = uiState.selectedMonth,
                        records = uiState.records,
                        habit = habit,
                        selectedDate = selectedDate,
                        today = dateProvider.today(),
                        onDateSelected = { date ->
                            selectedDate = date
                            coroutineScope.launch {
                                bottomSheetState.show()
                            }
                        },
                        onMonthChange = viewModel::changeMonth,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notification Settings
                    NotificationSettingsCard(
                        isEnabled = uiState.isNotificationEnabled,
                        notificationTime = uiState.notificationTime,
                        notificationError = uiState.notificationError,
                        onToggleEnabled = { enabled ->
                            viewModel.toggleNotification(enabled)
                        },
                        onTimeChanged = { time ->
                            viewModel.updateNotificationTime(time)
                        },
                        onErrorDismiss = {
                            viewModel.clearNotificationError()
                        },
                        onOpenSettings = {
                            viewModel.openAppSettings()
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // Activity Section
                    ActivitySection(
                        selectedTab = selectedActivityTab,
                        onTabChange = { selectedActivityTab = it },
                        records = uiState.records,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Bottom Sheet for date details
        selectedDate?.let { date ->
            val record = uiState.records.find { it.date == date }
            val habit = uiState.habit

            if (bottomSheetState.isVisible && habit != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        coroutineScope.launch {
                            bottomSheetState.hide()
                            selectedDate = null
                        }
                    },
                    sheetState = bottomSheetState
                ) {
                    DateDetailBottomSheet(
                        date = date,
                        habit = habit,
                        record = record,
                        onUpdateProgress = { value ->
                            viewModel.updateProgress(date, value, record?.note)
                        },
                        onUpdateNote = { note ->
                            viewModel.updateProgress(
                                date,
                                record?.completedCount ?: 0,
                                note
                            )
                        },
                        onDeleteRecord = {
                            viewModel.deleteRecord(date)
                            coroutineScope.launch {
                                bottomSheetState.hide()
                                selectedDate = null
                            }
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Habit?") },
                text = { Text("This action cannot be undone. All data associated with this habit will be permanently deleted.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteHabit()
                            onNavigateBack()
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ActivitySection(
    selectedTab: ActivityTab,
    onTabChange: (ActivityTab) -> Unit,
    records: List<HabitRecord>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { onTabChange(tab) },
                        label = { Text(tab.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { tab ->
                when (tab) {
                    ActivityTab.HISTORY -> {
                        HistoryList(records = records)
                    }
                    ActivityTab.NOTES -> {
                        NotesList(records = records.filter { !it.note.isNullOrBlank() })
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    records: List<HabitRecord>,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No history yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(modifier = modifier) {
            records.take(10).forEach { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        DateFormatter.formatShort(record.date),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${record.completedCount} completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesList(
    records: List<HabitRecord>,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No notes yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(modifier = modifier) {
            records.take(10).forEach { record ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            DateFormatter.formatShort(record.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            record.note ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateDetailBottomSheet(
    date: LocalDate,
    habit: Habit,
    record: HabitRecord?,
    onUpdateProgress: (Int) -> Unit,
    onUpdateNote: (String?) -> Unit,
    onDeleteRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    var note by remember(record) { mutableStateOf(record?.note ?: "") }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val isFuture = date > today

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    DateFormatter.formatLong(date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (record != null) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Section
            if (!isFuture) {
                if (habit.type == Habit.Type.COUNTABLE) {
                    CountableProgressSection(
                        currentValue = record?.completedCount ?: 0,
                        targetValue = habit.targetCount,
                        onValueChange = onUpdateProgress
                    )
                } else {
                    YesNoProgressSection(
                        isCompleted = record != null,
                        onToggle = { completed ->
                            onUpdateProgress(if (completed) 1 else 0)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Note Section
                Column {
                    Text(
                        "Note",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = {
                            note = it
                            onUpdateNote(it.takeIf { it.isNotBlank() })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Add a note...") },
                        maxLines = 3
                    )
                }
            }

            // Delete option for existing records
            if (record != null && !isFuture) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onDeleteRecord,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete record", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun CountableProgressSection(
    currentValue: Int,
    targetValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "Progress",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (currentValue > 0) onValueChange(currentValue - 1) }
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }

            Text(
                "$currentValue / $targetValue",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { onValueChange(currentValue + 1) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }

        LinearProgressIndicator(
            progress = (currentValue.toFloat() / targetValue.coerceAtLeast(1)),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun YesNoProgressSection(
    isCompleted: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "Completed",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = isCompleted,
                onClick = { onToggle(!isCompleted) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCompleted) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(if (isCompleted) "Completed" else "Mark as Complete")
                    }
                }
            )
        }
    }
}

@Composable
private fun handlePermissionRequest(viewModel: HabitDetailViewModel) {
    // This will be handled by platform-specific code
    // The ViewModel will emit an event that the Activity/ViewController can handle
}

@Composable
private fun handleOpenSettings() {
    // This will be handled by platform-specific code
    // The ViewModel already has openAppSettings() method that uses PermissionManager
}